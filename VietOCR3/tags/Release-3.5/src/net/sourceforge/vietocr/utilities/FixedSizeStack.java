/**
 * Copyright @ 2013 Quan Nguyen
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package net.sourceforge.vietocr.utilities;

import java.util.ArrayDeque;

/**
 * A fixed-size stack.
 *
 * @param <T>
 */
public class FixedSizeStack<T> extends ArrayDeque<T> {

    private int limit;

    public FixedSizeStack(int limit) {
        this.limit = limit;
    }

    @Override
    public void push(T obj) {
        super.push(obj);
        if (this.size() > limit) {
            super.removeLast();
        }
    }
}
