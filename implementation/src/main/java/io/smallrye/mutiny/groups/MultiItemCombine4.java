package io.smallrye.mutiny.groups;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;
import static io.smallrye.mutiny.helpers.ParameterValidation.size;

import java.util.concurrent.Flow.Publisher;

import io.smallrye.common.annotation.CheckReturnValue;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.tuples.Functions;
import io.smallrye.mutiny.tuples.Tuple4;

public class MultiItemCombine4<T1, T2, T3, T4> extends MultiItemCombineIterable {

    public MultiItemCombine4(Iterable<Publisher<?>> iterable) {
        super(iterable);
    }

    /**
     * Configures the combination to wait until all the {@link Publisher streams} to fire a completion or failure event
     * before propagating a failure downstream.
     *
     * @return the current {@link MultiItemCombine4}
     */
    @Override
    @CheckReturnValue
    public MultiItemCombine4<T1, T2, T3, T4> collectFailures() {
        super.collectFailures();
        return this;
    }

    /**
     * By default, the combination logic is called with one item of each observed stream. It <em>waits</em> until all
     * the upstreams emit an item, and it invokes the combination logic. In other words, it associated the items from
     * different streams having the same <em>index</em>. If one of the streams completes, the produced stream also
     * completes.
     * <p>
     * With this method, you can change this behavior and call the combination logic when one of the observed streams
     * emits an item. It calls the combination logic with this new item and the latest items emitted by the other
     * streams.
     * <p>
     * It waits until all the streams have emitted at least one item before calling the combination logic.
     * If one of the streams completes before emitting a value, the produced stream also completes without emitting an
     * item.
     *
     * @return the current {@link MultiItemCombine4}
     */
    @Override
    @CheckReturnValue
    public MultiItemCombine4<T1, T2, T3, T4> latestItems() {
        super.latestItems();
        return this;
    }

    /**
     * @return the resulting {@link Multi}. The items are combined into a {@link Tuple4 Tuple4&lt;T1, T2, T3, T4&gt;}.
     */
    @CheckReturnValue
    public Multi<Tuple4<T1, T2, T3, T4>> asTuple() {
        return using(Tuple4::of);
    }

    /**
     * Creates the resulting {@link Multi}. The items are combined using the given combinator function.
     *
     * @param combinator the combinator function, must not be {@code null}
     * @param <O> the type of item
     * @return the resulting {@link Multi}.
     */
    @SuppressWarnings("unchecked")
    @CheckReturnValue
    public <O> Multi<O> using(Functions.Function4<T1, T2, T3, T4, O> combinator) {
        nonNull(combinator, "combinator");
        return super.combine(args -> {
            size(args, 4, "args");
            return combinator.apply((T1) args.get(0), (T2) args.get(1), (T3) args.get(2), (T4) args.get(3));
        });
    }
}
