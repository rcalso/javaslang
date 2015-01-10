/**    / \____  _    ______   _____ / \____   ____  _____
 *    /  \__  \/ \  / \__  \ /  __//  \__  \ /    \/ __  \   Javaslang
 *  _/  // _\  \  \/  / _\  \\_  \/  // _\  \  /\  \__/  /   Copyright 2014-2015 Daniel Dietrich
 * /___/ \_____/\____/\_____/____/\___\_____/_/  \_/____/    Licensed under the Apache License, Version 2.0
 */
package javaslang.monad;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import javaslang.Algebra;
import javaslang.Algebra.Monad;
import javaslang.Tuple;
import javaslang.ValueObject;
import javaslang.monad.Option.None;
import javaslang.monad.Option.Some;
import javaslang.monad.Valences.Bivalent;

/**
 * An implementation similar to Scala's Try monad.
 *
 * @param <T> Value type in the case of success.
 */
public interface Try<T> extends Monad<T, Try<?>>, ValueObject, Bivalent<T, Throwable> {

	static <T> Try<T> of(Try.CheckedSupplier<T> supplier) {
		try {
			return new Success<>(supplier.get());
		} catch (Throwable t) {
			return new Failure<>(t);
		}
	}

	static <T> Try<Void> run(Try.CheckedRunnable runnable) {
		try {
			runnable.run();
			return new Success<>(null); // null represents the absence of an value, i.e. Void
		} catch (Throwable t) {
			return new Failure<>(t);
		}
	}

	boolean isFailure();

	boolean isSuccess();

	Try<T> recover(Function<Throwable, ? extends T> f);

	Try<T> recoverWith(Function<Throwable, Try<T>> f);

	Try<Throwable> failed();

	Try<T> filter(Predicate<? super T> predicate);

	void forEach(Consumer<? super T> action);

	@Override
	<U> Try<U> map(Function<? super T, ? extends U> mapper);

	@Override
	<U, TRY extends Algebra.HigherKinded<U, Try<?>>> Try<U> flatMap(Function<? super T, TRY> mapper);

	@Override
	boolean equals(Object o);

	@Override
	int hashCode();

	@Override
	String toString();

	// -- Try implementations

	public final class Success<T> implements Try<T> {

		private static final long serialVersionUID = 9157097743377386892L;

		private T value;

		public Success(T value) {
			this.value = value;
		}

		@Override
		public boolean isFailure() {
			return false;
		}

		@Override
		public boolean isSuccess() {
			return true;
		}

		@Override
		public T get() {
			return value;
		}

		@Override
		public T orElse(T other) {
			return value;
		}

		@Override
		public T orElseGet(Supplier<? extends T> other) {
			return value;
		}

		@Override
		public T orElseGet(Function<? super Throwable, ? extends T> other) {
			return value;
		}

		@Override
		public void orElseRun(Consumer<? super Throwable> action) {
			// nothing to do
		}

		@Override
		public <X extends Throwable> T orElseThrow(Supplier<X> exceptionSupplier) throws X {
			return value;
		}

		@Override
		public <X extends Throwable> T orElseThrow(Function<? super Throwable, X> exceptionProvider) throws X {
			return value;
		}

		@Override
		public Try<T> recover(Function<Throwable, ? extends T> f) {
			return this;
		}

		@Override
		public Try<T> recoverWith(Function<Throwable, Try<T>> f) {
			return this;
		}

		@Override
		public Option<T> toOption() {
			return new Some<>(value);
		}

		@Override
		public Either<Throwable, T> toEither() {
			return new Either.Right<>(value);
		}

		@Override
		public Try<T> filter(Predicate<? super T> predicate) {
			try {
				if (predicate.test(value)) {
					return this;
				} else {
					return new Failure<>(new NoSuchElementException("Predicate does not hold for " + value));
				}
			} catch (Throwable t) {
				return new Failure<>(t);
			}
		}

		@Override
		public Try<Throwable> failed() {
			return new Failure<>(new UnsupportedOperationException("Success.failed()"));
		}

		@Override
		public void forEach(Consumer<? super T> action) {
			action.accept(value);
		}

		@Override
		public <U> Try<U> map(Function<? super T, ? extends U> mapper) {
			try {
				return new Success<>(mapper.apply(value));
			} catch (Throwable t) {
				return new Failure<>(t);
			}
		}

		@SuppressWarnings("unchecked")
		@Override
		public <U, TRY extends Algebra.HigherKinded<U, Try<?>>> Try<U> flatMap(Function<? super T, TRY> mapper) {
			try {
				return (Try<U>) mapper.apply(value);
			} catch (Throwable t) {
				return new Failure<>(t);
			}
		}

		@Override
		public Tuple.Tuple1<T> unapply() {
			return Tuple.of(value);
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == this) {
				return true;
			}
			if (!(obj instanceof Success)) {
				return false;
			}
			final Success<?> success = (Success<?>) obj;
			return Objects.equals(value, success.value);
		}

		@Override
		public int hashCode() {
			return Objects.hashCode(value);
		}

		@Override
		public String toString() {
			return String.format("Success(%s)", value);
		}
	}

	public final class Failure<T> implements Try<T> {

		private static final long serialVersionUID = 2836756728630414146L;

		private final NonFatal cause;

		/**
		 * Constructs a Failure.
		 * 
		 * @param t A cause of type Throwable, may not be null.
		 * @throws NullPointerException if t is null
		 */
		public Failure(Throwable t) {
			Objects.requireNonNull(t, "Throwable is null");
			final Cause cause = Cause.of(t);
			if (cause.isFatal()) {
				throw cause;
			} else {
				this.cause = (NonFatal) cause;
			}
		}

		@Override
		public boolean isFailure() {
			return true;
		}

		@Override
		public boolean isSuccess() {
			return false;
		}

		// Throws NonFatal instead of Throwable because it is a RuntimeException which does not need to be checked.
		@Override
		public T get() throws NonFatal {
			throw cause;
		}

		@Override
		public T orElse(T other) {
			return other;
		}

		@Override
		public T orElseGet(Supplier<? extends T> other) {
			return other.get();
		}

		@Override
		public T orElseGet(Function<? super Throwable, ? extends T> other) {
			return other.apply(cause.getCause());
		}

		@Override
		public void orElseRun(Consumer<? super Throwable> action) {
			action.accept(cause.getCause());
		}

		@Override
		public <X extends Throwable> T orElseThrow(Supplier<X> exceptionSupplier) throws X {
			throw exceptionSupplier.get();
		}

		@Override
		public <X extends Throwable> T orElseThrow(Function<? super Throwable, X> exceptionProvider) throws X {
			throw exceptionProvider.apply(cause.getCause());
		}

		@Override
		public Try<T> recover(Function<Throwable, ? extends T> f) {
			return Try.of(() -> f.apply(cause.getCause()));
		}

		@Override
		public Try<T> recoverWith(Function<Throwable, Try<T>> f) {
			try {
				return f.apply(cause.getCause());
			} catch (Throwable t) {
				return new Failure<>(t);
			}
		}

		@Override
		public Option<T> toOption() {
			return None.instance();
		}

		@Override
		public Either<Throwable, T> toEither() {
			return new Either.Left<>(cause.getCause());
		}

		@Override
		public Try<Throwable> failed() {
			return new Success<>(cause.getCause());
		}

		@Override
		public Try<T> filter(Predicate<? super T> predicate) {
			return this;
		}

		@Override
		public void forEach(Consumer<? super T> action) {
			// nothing to do
		}

		@Override
		public <U> Try<U> map(Function<? super T, ? extends U> mapper) {
			@SuppressWarnings("unchecked")
			final Try<U> result = (Try<U>) this;
			return result;
		}

		@Override
		public <U, TRY extends Algebra.HigherKinded<U, Try<?>>> Try<U> flatMap(Function<? super T, TRY> mapper) {
			@SuppressWarnings("unchecked")
			final Try<U> result = (Try<U>) this;
			return result;
		}

		@Override
		public Tuple.Tuple1<Throwable> unapply() {
			return Tuple.of(cause.getCause());
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == this) {
				return true;
			}
			if (!(obj instanceof Failure)) {
				return false;
			}
			final Failure<?> failure = (Failure<?>) obj;
			return equals(cause.getCause(), failure.cause.getCause());
		}

		// DEV-NOTE: no cycle-detection, intentionally
		private boolean equals(Throwable thisCause, Throwable thatCause) {
			return (thisCause == thatCause)
					|| (thisCause != null
							&& thatCause != null
							&& Objects.equals(thisCause.getClass(), thatCause.getClass())
							&& Objects.equals(thisCause.getMessage(), thatCause.getMessage()) && equals(
								thisCause.getCause(), thatCause.getCause()));
		}

		@Override
		public int hashCode() {
			return Objects.hashCode(cause.getCause());
		}

		@Override
		public String toString() {
			return String.format("Failure(%s)", cause.getCause());
		}

		/**
		 * Causes wrap Throwables. They are unchecked, i.e. RuntimeExceptions, which are either fatal (represented by
		 * the subclass {@link Fatal}) or non-fatal (represented by the subclass {@link NonFatal}). Fatal causes are
		 * considered to be non-recoverable.
		 * <p>
		 * Use {@link Cause#of(Throwable)} to get an instance of Cause. The instance returned is either of type
		 * {@link Fatal} or {@link NonFatal}.
		 * <p>
		 * {@link #isFatal()} states, if this Cause is considered to be non-recoverable.
		 */
		public static abstract class Cause extends RuntimeException implements ValueObject {

			private static final long serialVersionUID = 1905549717320100279L;

			Cause(Throwable cause) {
				super(cause);
			}

			/**
			 * Returns true, if this is Fatal, i.e. if the cause is fatal. See {@link Cause#of(Throwable)} for the
			 * definition on when a Throwable is fatal.
			 * 
			 * @return true, if this instance is Fatal, false otherwise.
			 */
			public abstract boolean isFatal();

			@Override
			public Tuple.Tuple1<Throwable> unapply() {
				return Tuple.of(super.getCause());
			}

			/**
			 * Wraps t in a Cause which is either a {@link Fatal} or a {@link NonFatal}. The given Throwable t is
			 * wrapped in a Fatal, i.e. considered as a non-recoverable, if t is an instance of one of the following
			 * classes:
			 * 
			 * <ul>
			 * <li>InterruptedException</li>
			 * <li>LinkageError</li>
			 * <li>ThreadDeath</li>
			 * <li>VirtualMachineError (i.e. OutOfMemoryError)</li>
			 * </ul>
			 * 
			 * However, StackOverflowError is considered as a non-fatal.
			 * 
			 * @param t A Throwable
			 * @return A {@link Fatal}, if t is fatal, a {@link NonFatal} otherwise.
			 * @throws javaslang.Require.UnsatisfiedRequirementException
			 */
			public static Cause of(Throwable t) {
				Objects.requireNonNull(t, "Throwable is null");
				final boolean isFatal = (t instanceof VirtualMachineError && !(t instanceof StackOverflowError))
						|| t instanceof ThreadDeath
						|| t instanceof InterruptedException
						|| t instanceof LinkageError;
				return isFatal ? new Fatal(t) : new NonFatal(t);
			}
		}

		/**
		 * Use {@link Cause#of(Throwable)} to create instances of {@link Fatal} and {@link NonFatal}.
		 */
		public static final class Fatal extends Cause {

			private static final long serialVersionUID = 7927552082244515502L;

			Fatal(Throwable cause) {
				super(cause);
			}

			@Override
			public boolean isFatal() {
				return true;
			}
		}

		/**
		 * Use {@link Cause#of(Throwable)} to create instances of {@link Fatal} and {@link NonFatal}.
		 */
		public static final class NonFatal extends Cause {

			private static final long serialVersionUID = -1643015386682564223L;

			NonFatal(Throwable cause) {
				super(cause);
			}

			@Override
			public boolean isFatal() {
				return false;
			}
		}
	}

	// -- helpers

	/**
	 * Used to initialize a Try calling {@link Try#run(Try.CheckedRunnable)}.
	 */
	@FunctionalInterface
	static interface CheckedRunnable {

		/**
		 * Produces side-effects only, i.e. returns no value.
		 *
		 * @throws java.lang.Throwable if an error occurs.
		 */
		void run() throws Throwable;
	}

	/**
	 * Used to initialize a Try calling {@link Try#of(Try.CheckedSupplier)}.
	 *
	 * @param <T> Type of supplied object.
	 */
	@FunctionalInterface
	static interface CheckedSupplier<T> {

		/**
		 * Gets a result or throws a Throwable.
		 *
		 * @return a result
		 * @throws java.lang.Throwable if an error occurs.
		 */
		T get() throws Throwable;
	}
}
