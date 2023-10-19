/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.smithy.jmespath.ast;

import java.util.Objects;
import java.util.OptionalInt;
import software.amazon.smithy.jmespath.ExpressionVisitor;
import software.amazon.smithy.jmespath.JmespathExpression;

/**
 * Represents a slice expression, containing an optional zero-based
 * start offset, zero-based stop offset, and step.
 *
 * @see <a href="https://jmespath.org/specification.html#slices">Slices</a>
 */
public final class SliceExpression extends JmespathExpression {

    private final Integer start;
    private final Integer stop;
    private final int step;

    public SliceExpression(Integer start, Integer stop, int step) {
        this(start, stop, step, 1, 1);
    }

    public SliceExpression(Integer start, Integer stop, int step, int line, int column) {
        super(line, column);
        this.start = start;
        this.stop = stop;
        this.step = step;
    }

    @Override
    public <T> T accept(ExpressionVisitor<T> visitor) {
        return visitor.visitSlice(this);
    }

    public OptionalInt getStart() {
        return start == null ? OptionalInt.empty() : OptionalInt.of(start);
    }

    public OptionalInt getStop() {
        return stop == null ? OptionalInt.empty() : OptionalInt.of(stop);
    }

    public int getStep() {
        return step;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (!(o instanceof SliceExpression)) {
            return false;
        }
        SliceExpression sliceNode = (SliceExpression) o;
        return Objects.equals(getStart(), sliceNode.getStart())
               && Objects.equals(getStop(), sliceNode.getStop())
               && getStep() == sliceNode.getStep();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getStart(), getStop(), getStep());
    }

    @Override
    public String toString() {
        return "SliceExpression{start=" + start + ", stop=" + stop + ", step=" + step + '}';
    }
}
