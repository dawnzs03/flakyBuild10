/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.esql.expression.function.scalar.multivalue;

import org.elasticsearch.search.aggregations.metrics.CompensatedSum;
import org.elasticsearch.xpack.esql.planner.LocalExecutionPlanner;
import org.elasticsearch.xpack.ql.expression.Expression;
import org.elasticsearch.xpack.ql.tree.Source;
import org.elasticsearch.xpack.ql.type.DataType;
import org.elasticsearch.xpack.ql.type.DataTypes;
import org.hamcrest.Matcher;

import java.util.List;

import static org.elasticsearch.xpack.ql.util.NumericUtils.asLongUnsigned;
import static org.elasticsearch.xpack.ql.util.NumericUtils.unsignedLongToDouble;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

public class MvAvgTests extends AbstractMultivalueFunctionTestCase {
    @Override
    protected Expression build(Source source, Expression field) {
        return new MvAvg(source, field);
    }

    @Override
    protected DataType[] supportedTypes() {
        return representableNumerics();
    }

    @Override
    protected DataType expectedType(List<DataType> argTypes) {
        return DataTypes.DOUBLE;  // Averages are always a double
    }

    @Override
    protected Matcher<Object> resultMatcherForInput(List<?> input, DataType dataType) {
        return switch (LocalExecutionPlanner.toElementType(dataType)) {
            case DOUBLE -> {
                CompensatedSum sum = new CompensatedSum();
                for (Object i : input) {
                    sum.add((Double) i);
                }
                yield equalTo(sum.value() / input.size());
            }
            case INT -> equalTo(((double) input.stream().mapToInt(o -> (Integer) o).sum()) / input.size());
            case LONG -> {
                double sum;
                if (dataType == DataTypes.UNSIGNED_LONG) {
                    long accum = asLongUnsigned(0);
                    for (var l : input) {
                        accum = asLongUnsigned(accum + (long) l);
                    }
                    sum = unsignedLongToDouble(accum);
                } else {
                    sum = input.stream().mapToLong(o -> (Long) o).sum();
                }
                yield equalTo(sum / input.size());
            }
            case NULL -> nullValue();
            default -> throw new UnsupportedOperationException("unsupported type " + input);
        };
    }

    @Override
    protected String expectedEvaluatorSimpleToString() {
        return "MvAvg[field=Attribute[channel=0]]";
    }
}
