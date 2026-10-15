package org.crosswire.jsword;

import kotlin.coroutines.Continuation;
import kotlin.coroutines.EmptyCoroutineContext;
import kotlinx.coroutines.BuildersKt;
import org.crosswire.jsword.book.Book;
import org.crosswire.jsword.book.install.Installer;

/** Calls the suspend Installer API from the Java characterization tests (tests only; main code never blocks). */
public final class SuspendCalls {
    private SuspendCalls() {
    }

    /** One suspend call, written as a lambda over the continuation Kotlin adds to the signature. */
    public interface Call<T> {
        Object run(Continuation<? super T> continuation) throws Exception;
    }

    @SuppressWarnings("unchecked")
    public static <T> T blocking(Call<T> call) throws Exception {
        return (T) BuildersKt.runBlocking(EmptyCoroutineContext.INSTANCE, (scope, continuation) -> {
            try {
                return call.run((Continuation<? super T>) continuation);
            } catch (Exception e) {
                throw SuspendCalls.<RuntimeException>sneaky(e); // keeps the checked InstallException the tests expect
            }
        });
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable> E sneaky(Throwable error) throws E {
        throw (E) error;
    }

    public static void reload(Installer installer) throws Exception {
        blocking(c -> installer.reloadBookList(c));
    }

    public static void install(Installer installer, Book book) throws Exception {
        blocking(c -> installer.install(book, c));
    }

    public static void install(Installer installer, Book book, String jobId) throws Exception {
        blocking(c -> installer.install(book, jobId, c));
    }

    public static boolean isNewer(Installer installer, Book book) throws Exception {
        return SuspendCalls.<Boolean>blocking(c -> installer.isNewer(book, c));
    }

    public static Long indexLastUpdated(Installer installer) throws Exception {
        return SuspendCalls.<Long>blocking(c -> installer.indexLastUpdated(c));
    }
}
