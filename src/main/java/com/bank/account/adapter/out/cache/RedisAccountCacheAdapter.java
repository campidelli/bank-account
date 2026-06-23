package com.bank.account.adapter.out.cache;

import com.bank.account.domain.model.Account;
import com.bank.account.domain.port.out.AccountCachePort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Component
public class RedisAccountCacheAdapter implements AccountCachePort {

    private static final String KEY_PREFIX = "accounts:";
    private static final Duration TTL = Duration.ofMinutes(10);

    private final RedisTemplate<String, Object> redisTemplate;

    public RedisAccountCacheAdapter(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Optional<Account> get(String accountNumber) {
        Object value = redisTemplate.opsForValue().get(key(accountNumber));
        if (value == null) {
            return Optional.empty();
        }
        return Optional.of((Account) value);
    }

    @Override
    public void put(String accountNumber, Account account) {
        redisTemplate.opsForValue().set(
                key(accountNumber),
                account,
                TTL
        );
    }

    @Override
    public void evict(String accountNumber) {
        redisTemplate.delete(key(accountNumber));
    }

    @Override
    public void clear() {
        redisTemplate.delete(redisTemplate.keys(KEY_PREFIX + "*"));
    }

    private String key(String accountNumber) {
        return KEY_PREFIX + accountNumber;
    }
}