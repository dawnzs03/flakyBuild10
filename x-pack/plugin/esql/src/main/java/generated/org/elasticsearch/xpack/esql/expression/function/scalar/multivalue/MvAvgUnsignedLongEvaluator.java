// Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
// or more contributor license agreements. Licensed under the Elastic License
// 2.0; you may not use this file except in compliance with the Elastic License
// 2.0.
package org.elasticsearch.xpack.esql.expression.function.scalar.multivalue;

import java.lang.Override;
import java.lang.String;
import org.elasticsearch.compute.data.Block;
import org.elasticsearch.compute.data.DoubleArrayVector;
import org.elasticsearch.compute.data.DoubleBlock;
import org.elasticsearch.compute.data.LongBlock;
import org.elasticsearch.compute.data.Vector;
import org.elasticsearch.compute.operator.EvalOperator;

/**
 * {@link EvalOperator.ExpressionEvaluator} implementation for {@link MvAvg}.
 * This class is generated. Do not edit it.
 */
public final class MvAvgUnsignedLongEvaluator extends AbstractMultivalueFunction.AbstractEvaluator {
  public MvAvgUnsignedLongEvaluator(EvalOperator.ExpressionEvaluator field) {
    super(field);
  }

  @Override
  public String name() {
    return "MvAvg";
  }

  @Override
  public Block evalNullable(Block fieldVal) {
    LongBlock v = (LongBlock) fieldVal;
    int positionCount = v.getPositionCount();
    DoubleBlock.Builder builder = DoubleBlock.newBlockBuilder(positionCount);
    for (int p = 0; p < positionCount; p++) {
      int valueCount = v.getValueCount(p);
      if (valueCount == 0) {
        builder.appendNull();
        continue;
      }
      int first = v.getFirstValueIndex(p);
      if (valueCount == 1) {
        long value = v.getLong(first);
        double result = MvAvg.singleUnsignedLong(value);
        builder.appendDouble(result);
        continue;
      }
      int end = first + valueCount;
      long value = v.getLong(first);
      for (int i = first + 1; i < end; i++) {
        long next = v.getLong(i);
        value = MvAvg.processUnsignedLong(value, next);
      }
      double result = MvAvg.finishUnsignedLong(value, valueCount);
      builder.appendDouble(result);
    }
    return builder.build();
  }

  @Override
  public Vector evalNotNullable(Block fieldVal) {
    LongBlock v = (LongBlock) fieldVal;
    int positionCount = v.getPositionCount();
    double[] values = new double[positionCount];
    for (int p = 0; p < positionCount; p++) {
      int valueCount = v.getValueCount(p);
      int first = v.getFirstValueIndex(p);
      if (valueCount == 1) {
        long value = v.getLong(first);
        double result = MvAvg.singleUnsignedLong(value);
        values[p] = result;
        continue;
      }
      int end = first + valueCount;
      long value = v.getLong(first);
      for (int i = first + 1; i < end; i++) {
        long next = v.getLong(i);
        value = MvAvg.processUnsignedLong(value, next);
      }
      double result = MvAvg.finishUnsignedLong(value, valueCount);
      values[p] = result;
    }
    return new DoubleArrayVector(values, positionCount);
  }

  @Override
  public Block evalSingleValuedNullable(Block fieldVal) {
    LongBlock v = (LongBlock) fieldVal;
    int positionCount = v.getPositionCount();
    DoubleBlock.Builder builder = DoubleBlock.newBlockBuilder(positionCount);
    for (int p = 0; p < positionCount; p++) {
      int valueCount = v.getValueCount(p);
      if (valueCount == 0) {
        builder.appendNull();
        continue;
      }
      assert valueCount == 1;
      int first = v.getFirstValueIndex(p);
      long value = v.getLong(first);
      double result = MvAvg.singleUnsignedLong(value);
      builder.appendDouble(result);
    }
    return builder.build();
  }

  @Override
  public Vector evalSingleValuedNotNullable(Block fieldVal) {
    LongBlock v = (LongBlock) fieldVal;
    int positionCount = v.getPositionCount();
    double[] values = new double[positionCount];
    for (int p = 0; p < positionCount; p++) {
      int valueCount = v.getValueCount(p);
      assert valueCount == 1;
      int first = v.getFirstValueIndex(p);
      long value = v.getLong(first);
      double result = MvAvg.singleUnsignedLong(value);
      values[p] = result;
    }
    return new DoubleArrayVector(values, positionCount);
  }
}
