/*
 * Copyright (c) "Neo4j"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.gds.ml.decisiontree;

import java.util.Objects;

public class DecisionTreePredictor<PREDICTION extends Number> {

    public final TreeNode<PREDICTION> root;

    public DecisionTreePredictor(TreeNode<PREDICTION> root) {
        this.root = root;
    }

    public PREDICTION predict(double[] features) {
        assert features.length > 0;

        TreeNode<PREDICTION> node = root;

        while (node.leftChild() != null) {
            assert features.length > node.featureIndex();
            assert node.rightChild() != null;

            if (features[node.featureIndex()] < node.thresholdValue()) {
                node = node.leftChild();
            } else {
                node = node.rightChild();
            }
        }

        return node.prediction();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DecisionTreePredictor<?> that = (DecisionTreePredictor<?>) o;
        return root.equals(that.root);
    }

    @Override
    public int hashCode() {
        return Objects.hash(root);
    }


}
