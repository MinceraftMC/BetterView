package dev.booky.betterview.api;
// Created by booky10 in BetterView (5:54 PM 09.04.2026)

import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NullMarked;

import java.util.concurrent.Executor;

/**
 * This executor targets only a single thread to ensure thread-safety
 * for API access. The underlying executor may (or may not) immediately run scheduled
 * {@link Runnable}'s if the current thread is already correct.
 */
@NullMarked
public interface BvExecutor extends Executor {

    /**
     * @return whether this method is called on the correct thread already.
     */
    @Contract(pure = true)
    boolean isSameThread();
}
