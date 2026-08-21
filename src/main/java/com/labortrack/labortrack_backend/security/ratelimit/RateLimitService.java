package com.labortrack.labortrack_backend.security.ratelimit;

import io.github.bucket4j.Bucket;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * This class provides in-memory rate-limiting
 * functionality. It creates and stores buckets
 * for different request types, such as login,
 * registration, password change, and  global
 * requests. Each request consumes one token
 * from its assigned bucket. If no token is
 * available, the configured rate limit has
 * been reached.
 */
@Service
public class RateLimitService {

    private final ConcurrentMap<String, Bucket> buckets =
            new ConcurrentHashMap<>();

    /**
     * This method gets or creates a rate-limit bucket
     * for the provided key, then attempts to consume
     * one request from that bucket. This method would
     * return true when the requests are allowed, false
     * when the rate limit has been reached.
     */
    public boolean tryConsume(
            String key,
            RateLimitProperties.Limit limit
    ) {

        // get the bucket using the key. If key not exists, create bucket with key
        Bucket bucket = buckets.computeIfAbsent(
                key,
                ignored -> createBucket(limit)
        );

        // subtract one to the bucket
        return bucket.tryConsume(1);
    }

    // HELPER METHODS

    /**
     * This method helps create one in-memory token bucket
     * using the configured request capacity and refill
     * duration.
     */
    private Bucket createBucket(
            RateLimitProperties.Limit limit
    ) {
        return Bucket
                .builder()
                .addLimit(bandwidth -> bandwidth
                        .capacity(limit.requests())
                        .refillGreedy(
                                limit.requests(),
                                limit.duration()
                        )
                )
                .build();
    }

}
