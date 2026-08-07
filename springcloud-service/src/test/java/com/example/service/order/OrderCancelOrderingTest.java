package com.example.service.order;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class OrderCancelOrderingTest {

    @Test
    public void cancelWaitsUntilOrderIsSent() throws Exception {
        FakeOrderSender sender = new FakeOrderSender();
        CountDownLatch start = new CountDownLatch(1);

        Thread orderThread = new Thread(() -> {
            await(start);
            sender.saveOrder("W001");
            sender.sendPendingByOrderId("W001");
        });
        Thread cancelThread = new Thread(() -> {
            await(start);
            sender.saveCancel("W001");
            sender.sendPendingByOrderId("W001");
        });

        orderThread.start();
        cancelThread.start();
        start.countDown();
        orderThread.join();
        cancelThread.join();

        Assert.assertEquals(list("ORDER:W001", "CANCEL:W001"), sender.sentMessages);
    }

    @Test
    public void cancelArrivesFirstAndIsSentByLaterOrderTrigger() throws Exception {
        FakeOrderSender sender = new FakeOrderSender();

        sender.saveCancel("W002");
        sender.sendPendingByOrderId("W002");
        Assert.assertEquals(Collections.emptyList(), sender.sentMessages);

        sender.saveOrder("W002");
        sender.sendPendingByOrderId("W002");
        Assert.assertEquals(list("ORDER:W002", "CANCEL:W002"), sender.sentMessages);
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await(3, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static List<String> list(String... values) {
        List<String> result = new ArrayList<>();
        Collections.addAll(result, values);
        return result;
    }

    private static class FakeOrderSender {
        private final Map<String, Boolean> orderSaved = new ConcurrentHashMap<>();
        private final Map<String, Boolean> orderSent = new ConcurrentHashMap<>();
        private final Map<String, Boolean> cancelSaved = new ConcurrentHashMap<>();
        private final Map<String, Boolean> cancelSent = new ConcurrentHashMap<>();
        private final Map<String, Object> locks = new ConcurrentHashMap<>();
        private final List<String> sentMessages = Collections.synchronizedList(new ArrayList<>());

        void saveOrder(String orderId) {
            orderSaved.put(orderId, true);
        }

        void saveCancel(String orderId) {
            cancelSaved.put(orderId, true);
        }

        void sendPendingByOrderId(String orderId) {
            Object lock = locks.computeIfAbsent(orderId, key -> new Object());
            synchronized (lock) {
                if (Boolean.TRUE.equals(orderSaved.get(orderId)) && !Boolean.TRUE.equals(orderSent.get(orderId))) {
                    sleep(100);
                    sentMessages.add("ORDER:" + orderId);
                    orderSent.put(orderId, true);
                }
                if (Boolean.TRUE.equals(orderSent.get(orderId))
                        && Boolean.TRUE.equals(cancelSaved.get(orderId))
                        && !Boolean.TRUE.equals(cancelSent.get(orderId))) {
                    sentMessages.add("CANCEL:" + orderId);
                    cancelSent.put(orderId, true);
                }
            }
        }

        private void sleep(long millis) {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
