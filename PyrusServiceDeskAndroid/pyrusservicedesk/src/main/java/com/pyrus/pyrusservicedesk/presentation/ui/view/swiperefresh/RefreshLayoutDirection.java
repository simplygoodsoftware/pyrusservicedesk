package com.pyrus.pyrusservicedesk.presentation.ui.view.swiperefresh;

/**
 *
 * The MIT License (MIT)
 * Copyright (c) 2015 OrangeGangsters
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 * Class taken from https://github.com/omadahealth/SwipyRefreshLayout. v.1.2.3.
 */
public enum RefreshLayoutDirection {

    TOP(0),
    BOTTOM(1),
    BOTH(2);

    private int mValue;

    RefreshLayoutDirection(int value) {
        this.mValue = value;
    }

    public static RefreshLayoutDirection getFromInt(int value) {
        for (RefreshLayoutDirection direction : RefreshLayoutDirection.values()) {
            if (direction.mValue == value) {
                return direction;
            }
        }
        return BOTH;
    }

}
