package com.danilkinkin.buckwheat.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.danilkinkin.buckwheat.data.entities.Subscription
import com.danilkinkin.buckwheat.data.entities.SubscriptionPeriod
import com.danilkinkin.buckwheat.di.SpendsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class SubscriptionsViewModel @Inject constructor(
    private val spendsRepository: SpendsRepository,
) : ViewModel() {
    val subscriptions = spendsRepository.getAllSubscriptions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val priceChanges = spendsRepository.getAllPriceChanges()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun add(
        name: String,
        amount: BigDecimal,
        period: SubscriptionPeriod,
        intervalCount: Int,
        nextDue: Date,
        categoryId: Int?,
        serviceId: String?,
    ) {
        viewModelScope.launch {
            spendsRepository.addSubscription(
                name,
                amount,
                period,
                intervalCount,
                nextDue,
                categoryId,
                serviceId,
            )
        }
    }

    fun update(subscription: Subscription) {
        viewModelScope.launch {
            spendsRepository.updateSubscription(subscription)
        }
    }

    fun delete(subscriptionId: Int) {
        viewModelScope.launch {
            spendsRepository.deleteSubscription(subscriptionId)
        }
    }

    fun restore(subscription: Subscription) {
        viewModelScope.launch {
            spendsRepository.restoreSubscription(subscription)
        }
    }

    fun skipCycle(subscription: Subscription) {
        viewModelScope.launch {
            spendsRepository.skipSubscriptionCycle(subscription)
        }
    }

    fun markPaid(subscription: Subscription) {
        viewModelScope.launch {
            spendsRepository.markSubscriptionPaid(subscription)
        }
    }
}
