/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.discovery.etcd.backoff;

/**
 * Power of two function determined by the min, max values and the number of steps between them.
 */
public class SquareBackOff implements BackOff {

    private final long max;

    private final long min;

    private final int steps;

    private final double b;

    private int step;

    public SquareBackOff(long min, long max, int steps) {
        if (min < 0) {
            throw new IllegalArgumentException("min must be greater or equal to 0");
        }
        if (max < min) {
            throw new IllegalArgumentException("max must be greater or equal to min");
        }
        if (steps < 0) {
            throw new IllegalArgumentException("steps must be greater than 0");
        }
        this.min = min;
        this.max = max;
        this.steps = steps;
        b = steps / Math.sqrt(max - min);
    }

    public long value() {
        return value(step);
    }

    public long value(int index) {
        if (index < 0) {
            throw new IllegalArgumentException("index must be greater or equal than 0");
        }
        long value = (int) (min + Math.pow((index / b), 2.0D));
        return Math.min(max, Math.max(min, value));
    }

    public long increment() {
        long previous = value();
        step = Math.min(step + 1, steps);
        return previous;
    }

    public long reset() {
        long previous = value();
        step = 0;
        return previous;
    }

    public long max() {
        return max;
    }

    @Override
    public String toString() {
        return "SquareBackOff{" +
                "steps=" + steps +
                ", max=" + max +
                ", min=" + min +
                '}';
    }
}
