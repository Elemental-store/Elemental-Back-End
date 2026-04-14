package com.elemental.backend.service;

import com.stripe.Stripe;
import com.stripe.model.Customer;
import com.stripe.model.PaymentMethod;
import com.stripe.model.PaymentMethodCollection;
import com.stripe.model.SetupIntent;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.CustomerListPaymentMethodsParams;
import com.stripe.param.SetupIntentCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class StripeService {

    public StripeService(@Value("${stripe.secret-key}") String secretKey) {
        Stripe.apiKey = secretKey;
    }

    public String createCustomer(String email, String name) {
        try {
            CustomerCreateParams params = CustomerCreateParams.builder()
                    .setEmail(email)
                    .setName(name)
                    .build();
            Customer customer = Customer.create(params);
            return customer.getId();
        } catch (Exception e) {
            throw new RuntimeException("Error creando Stripe Customer: " + e.getMessage());
        }
    }

    public String createSetupIntent(String customerId) {
        try {
            SetupIntentCreateParams params = SetupIntentCreateParams.builder()
                    .setCustomer(customerId)
                    .addPaymentMethodType("card")
                    .build();
            SetupIntent intent = SetupIntent.create(params);
            return intent.getClientSecret();
        } catch (Exception e) {
            throw new RuntimeException("Error creando SetupIntent: " + e.getMessage());
        }
    }

    public List<Map<String, Object>> listPaymentMethods(String customerId) {
        try {
            CustomerListPaymentMethodsParams params = CustomerListPaymentMethodsParams.builder()
                    .setType(CustomerListPaymentMethodsParams.Type.CARD)
                    .build();

            Customer customer = Customer.retrieve(customerId);
            PaymentMethodCollection methods = customer.listPaymentMethods(params);

            List<Map<String, Object>> result = new ArrayList<>();
            for (PaymentMethod pm : methods.getData()) {
                PaymentMethod.Card card = pm.getCard();
                Map<String, Object> entry = new HashMap<>();
                entry.put("id",       pm.getId());
                entry.put("brand",    card.getBrand());
                entry.put("last4",    card.getLast4());
                entry.put("expMonth", card.getExpMonth());
                entry.put("expYear",  card.getExpYear());
                result.add(entry);
            }
            return result;

        } catch (Exception e) {
            throw new RuntimeException("Error listando métodos de pago: " + e.getMessage());
        }
    }

    public void detachPaymentMethod(String paymentMethodId) {
        try {
            PaymentMethod pm = PaymentMethod.retrieve(paymentMethodId);
            pm.detach();
        } catch (Exception e) {
            throw new RuntimeException("Error eliminando método de pago: " + e.getMessage());
        }
    }
}