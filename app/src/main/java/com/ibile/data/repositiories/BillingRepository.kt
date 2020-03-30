package com.ibile.data.repositiories

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import com.ibile.data.SharedPref
import com.ibile.data.repositiories.BillingRepository.Companion.SUBSCRIPTION_SKUS
import com.jakewharton.rxrelay2.BehaviorRelay
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.SingleEmitter

/**
 * The [BillingRepository] class, because of how the [BillingClient] operates, may encounter some
 * logic issues with varying severity but they are not handled because they are not meant to happen
 * often.
 *
 * The idea of this fun is to log such issues to the server (probably to Firebase crashlytics) and if
 * seen to be appearing often, get fixed.
 * @param msg
 */
private fun logUnexpectedConditionToServer(msg: String = "") {
    // TODO: log unexpected condition to server (not expecting this to happen)
    Log.d(BillingRepository.TAG, msg)
}

/**
 * Abstracts all billing logic and exposes billing (in-app purchases) states to clients.
 *
 * Clients should ensure to call [startConnection] or else there is no guarantee that states subscribed
 * to will emit data.
 *
 * This is not near the best implementation for this repository, but the app is still small at this
 * level and only has states for app access subscriptions (quarterly/annually) [SUBSCRIPTION_SKUS]
 * and the implementation is sufficient for now.
 *
 * @property sharedPref
 * @constructor
 *
 * @param context
 */
class BillingRepository(context: Context, private val sharedPref: SharedPref) :
    PurchasesUpdatedListener,
    BillingClientStateListener {

    /**
     * Exposes subscription (access subscription @see [SUBSCRIPTION_SKUS]) status to clients
     *
     * To keep it simple, a property is used for exposing the subscription status.
     * A more sophisticated solution would be to store the status in the database with details of
     * the subscription.
     */
    private val _subscriptionStatus = BehaviorRelay.create<SubscriptionStatus>()
    val subscriptionStatus: Observable<SubscriptionStatus>
        get() = _subscriptionStatus.hide()

    private val _serviceStatus = BehaviorRelay.create<ServiceStatus>()

    /**
     * Exposes a service status to clients with which they can manually reconnect when service
     * is [ServiceStatus.Disconnected].
     *
     * Clients should use [reconnect] to reestablish connection to the billing repository.
     * A service level of [ServiceStatus.CannotReconnect] means reconnection attempt cannot be made
     * again and a client should maybe restart the app after (reconnection attempt has been made
     * multiple times and same result is being returned. This is most likely an issue with Google
     * Play on the user's device).
     *
     * @see [onBillingServiceDisconnected]
     */
    val serviceStatus: Observable<ServiceStatus>
        get() = _serviceStatus.hide()

    private var reconnectionRetryCount = 0
        set(value) {
            when {
                value == 0 -> _serviceStatus.accept(ServiceStatus.Connected)
                value in 1..5 -> _serviceStatus.accept(ServiceStatus.Disconnected)
                value > 5 -> _serviceStatus.accept(ServiceStatus.CannotReconnect)
            }
            field = value
        }

    private val billingClient = BillingClient.newBuilder(context)
        .enablePendingPurchases()
        .setListener(this)
        .build()


    override fun onPurchasesUpdated(result: BillingResult?, purchaseList: MutableList<Purchase>?) {
        when (result?.responseCode) {
            BillingClient.BillingResponseCode.OK -> processPurchases(purchaseList)
            BillingClient.BillingResponseCode.USER_CANCELED -> processPurchases(null)
            else -> logUnexpectedConditionToServer(result?.debugMessage ?: "")
        }
    }

    override fun onBillingSetupFinished(billingResult: BillingResult?) {
        reconnectionRetryCount = 0
        val purchasesResult = billingClient.queryPurchases(BillingClient.SkuType.SUBS)
        processPurchases(purchasesResult.purchasesList)
    }

    /**
     * @see (https://developer.android.com/google/play/billing/billing_library_overview#Connect)
     */
    override fun onBillingServiceDisconnected() {
        logUnexpectedConditionToServer()
        if (_serviceStatus.value !is ServiceStatus.CannotReconnect) reconnectionRetryCount += 1
    }

    /**
     * This function is like the entry point for [BillingRepository]. It distributes the different
     * billings that may exist to their subsequent processors. Processors are meant to retrieve the
     * billings they are interested in (using their SKUs) and update their respective billing states.
     *
     * Currently, only subscription billings are available. As the app grows, Billing processors can
     * be abstracted into classes where their status could be exposed individually instead of using
     * this class as it is currently done.
     *
     * @param purchasesList
     */
    private fun processPurchases(purchasesList: MutableList<Purchase>?) {
        val subscriptionPurchases = purchasesList?.filter { SUBSCRIPTION_SKUS.contains(it.sku) }
        processSubscriptionPurchases(subscriptionPurchases)
    }

    private fun processSubscriptionPurchases(subscriptions: List<Purchase>?) {
        if (subscriptions.isNullOrEmpty()) {
            logUnexpectedConditionToServer()
            _subscriptionStatus.accept(SubscriptionStatus.InActive)
            return
        }
        // user should only have one of the purchases ideally
        // this can only happen if the user subscribes to two plans in the same category
        // e.g. in this context, user subscribes to quarterly and then somehow subscribes to
        // annual (can only happen if there is a flaw in this logic that displays the subscription
        // screen to the user and allows the user to still go ahead with subscribing or if user
        // subscribes externally without knowing they have one active subscription already)
        val subscription = if (subscriptions.size > 1) {
            logUnexpectedConditionToServer()
            getMostRecentSubscription(subscriptions)
        } else subscriptions[0]

        when (subscription.purchaseState) {
            Purchase.PurchaseState.PURCHASED -> handleSubscriptionIsPurchased(subscription)
            Purchase.PurchaseState.PENDING -> handleSubscriptionIsPending()
            Purchase.PurchaseState.UNSPECIFIED_STATE ->
                _subscriptionStatus.accept(SubscriptionStatus.InActive)
        }
    }

    private fun getMostRecentSubscription(subscriptions: List<Purchase>): Purchase {
        return subscriptions.reduce { acc, purchase ->
            if (purchase.purchaseTime > acc.purchaseTime) purchase else acc
        }
    }

    private fun handleSubscriptionIsPending() {
        logUnexpectedConditionToServer()
        // refer to docs on why this could happen:
        // https://developer.android.com/google/play/billing/billing_library_overview#pending
    }

    private fun handleSubscriptionIsPurchased(subscription: Purchase) {
        if (!subscriptionIsVerified(subscription)) {
            verifySubscription(subscription)
        } else {
            acknowledgeSubscription(subscription)
        }
    }

    /**
     * This function should ideally verify the subscription as described in the docs. However, the
     * project does not use a backend yet and since the other method of verification is not secure,
     * it is unnecessary to try to use it.
     *
     * @see (https://developer.android.com/google/play/billing/billing_library_overview#Verify-purchase)
     * https://developer.android.com/google/play/billing/billing_library_overview#Enable
     *
     * @param subscription - purchase object representing a subscription
     */
    private fun verifySubscription(subscription: Purchase) {
        sharedPref.subscriptionToken = subscription.purchaseToken
        acknowledgeSubscription(subscription)
    }

    private fun acknowledgeSubscription(subscription: Purchase) {
        if (subscription.isAcknowledged) {
            _subscriptionStatus.accept(SubscriptionStatus.Active)
        } else {
            try {
                useBillingClient {
                    val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(subscription.purchaseToken)
                    this.acknowledgePurchase(acknowledgePurchaseParams.build()) {
                        _subscriptionStatus.accept(SubscriptionStatus.Active)
                    }
                }
            } catch (ex: ServiceAvailabilityException) {
                // billing client is not available but there is nothing to do here since client
                // connection is retried in [onBillingServiceDisconnected], and this method gets called
                // again if retry is successful
            }
        }
    }

    private fun subscriptionIsVerified(subscription: Purchase): Boolean {
        return sharedPref.subscriptionToken == subscription.purchaseToken
    }

    /**
     * Entry point for clients.
     *
     * @see [BillingRepository]
     */
    fun startConnection() {
        // if connection is already started
        if (billingClient.isReady) return
        if (_serviceStatus.value is ServiceStatus.CannotReconnect) return
        billingClient.startConnection(this)
    }

    fun reconnect() {
        startConnection()
    }

    fun getSubscriptionsSkusDetails(): Single<List<SkuDetails>> {
        val skuDetailsParams = SkuDetailsParams
            .newBuilder()
            .setSkusList(SUBSCRIPTION_SKUS)
            .setType(BillingClient.SkuType.SUBS)
            .build()
        return Single.create { emitter: SingleEmitter<List<SkuDetails>> ->
            try {
                useBillingClient {
                    this.querySkuDetailsAsync(skuDetailsParams) { _: BillingResult?, subscriptionSkus: MutableList<SkuDetails>? ->
                        emitter.onSuccess(subscriptionSkus.orEmpty())
                    }
                }
            } catch (ex: ServiceAvailabilityException) {
                emitter.onError(ex)
            }
        }
    }

    @Throws(ServiceAvailabilityException::class)
    fun launchSubscriptionBillingFlow(activity: Activity, subscriptionSkuDetails: SkuDetails) {
        useBillingClient {
            if (this.isFeatureSupported(BillingClient.FeatureType.SUBSCRIPTIONS).responseCode != BillingClient.BillingResponseCode.OK) {
                // refer to docs for why this could happen:
                // https://developer.android.com/google/play/billing/billing_library_overview#Enable
                logUnexpectedConditionToServer()
                throw ServiceAvailabilityException()
            }
            val billingFlowParams = BillingFlowParams
                .newBuilder()
                .setSkuDetails(subscriptionSkuDetails)
                .build()
            this.launchBillingFlow(activity, billingFlowParams)
        }
    }

    private fun useBillingClient(operation: BillingClient.() -> Unit) {
        if (billingClient.isReady) operation(billingClient)
        else throw ServiceAvailabilityException()
    }

    companion object {
        private const val ANNUAL_ACCESS_SKU = "ibilemaps_access_sub"
        private const val QUARTERLY_ACCESS_SKU = "ibile_maps_access_sub_quartely"
        val SUBSCRIPTION_SKUS = listOf(ANNUAL_ACCESS_SKU, QUARTERLY_ACCESS_SKU)

        val TAG = BillingRepository::class.java.simpleName
    }

    sealed class SubscriptionStatus {
        object InActive : SubscriptionStatus()
        object Active : SubscriptionStatus()

        val isActive: Boolean
            get() {
                return this is Active
            }
    }

    sealed class ServiceStatus {
        object Connected : ServiceStatus()
        object Disconnected : ServiceStatus()
        object CannotReconnect : ServiceStatus()
    }

    class ServiceAvailabilityException : Exception()
}
