/*
 * Firebase UI Bindings Android Library
 *
 * Copyright © 2015 Firebase - All Rights Reserved
 * https://www.firebase.com
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binaryform must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY FIREBASE AS IS AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO
 * EVENT SHALL FIREBASE BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.firebase.ui;

import com.firebase.client.*;
import com.firebase.client.core.utilities.Predicate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * This class implements an array-like collection on top of a Firebase location.
 */
class FirebaseArray implements ChildEventListener {
    public enum Order { Ascending, Descending }

    public interface OnChangedListener {
        enum EventType { Added, Changed, Removed, Moved, All }
        void onChanged(EventType type, int index, int oldIndex);
    }

    private Query mQuery;
    private OnChangedListener mListener;
    private ArrayList<DataSnapshot> mSnapshots;

    public FirebaseArray(Query ref) {
        mQuery = ref;
        mSnapshots = new ArrayList<DataSnapshot>();
        mQuery.addChildEventListener(this);
    }

    public void cleanup() {
        mQuery.removeEventListener(this);
    }

    public int getCount() {
        return mSnapshots.size();

    }
    public DataSnapshot getItem(int index) {
        return mSnapshots.get(index);
    }

    private int getIndexForKey(String key) {
        int index = 0;
        for (int i = 0; i < mSnapshots.size(); i++) {
            if (mSnapshots.get(i).getKey().equals(key)) {
                return index;
            } else {
                index++;
            }
        }
        throw new IllegalArgumentException("Key not found");
    }

    // Start of ChildEventListener methods
    @Override
    public void onChildAdded(DataSnapshot snapshot, String previousChildKey) {
        // TODO if sorted, we need to add in a sorted place...right?
        int index = 0;

        if (mPredicate != null) {
           if (mPredicate.evaluate(snapshot)) {
               // Don't add.
               // TODO do in changed method too
               return;
           }
        }

        if (mComparator != null) {
            index = Collections.binarySearch(mSnapshots, snapshot, mComparator);
            if (index < 0) {
                index = (index + 1) * -1;
            }
        }
        else if (previousChildKey != null) {
            index = getIndexForKey(previousChildKey) + 1;
        }
        mSnapshots.add(index, snapshot);
        notifyChangedListeners(OnChangedListener.EventType.Added, index);
    }

    @Override
    public void onChildChanged(DataSnapshot snapshot, String previousChildKey) {
        int index = getIndexForKey(snapshot.getKey());
        mSnapshots.set(index, snapshot);
        Collections.sort(mSnapshots, mComparator);
        notifyChangedListeners(OnChangedListener.EventType.Changed, index);
    }

    @Override
    public void onChildRemoved(DataSnapshot snapshot) {
        int index = getIndexForKey(snapshot.getKey());
        mSnapshots.remove(index);
        notifyChangedListeners(OnChangedListener.EventType.Removed, index);
    }

    @Override
    public void onChildMoved(DataSnapshot snapshot, String previousChildKey) {
        // TODO if sorted we don't care
        int oldIndex = getIndexForKey(snapshot.getKey());
        mSnapshots.remove(oldIndex);
        int newIndex = previousChildKey == null ? 0 : (getIndexForKey(previousChildKey) + 1);
        mSnapshots.add(newIndex, snapshot);
        Collections.sort(mSnapshots, mComparator);
        notifyChangedListeners(OnChangedListener.EventType.Moved, newIndex, oldIndex);
    }

    @Override
    public void onCancelled(FirebaseError firebaseError) {
        // TODO: what do we do with this?
    }
    // End of ChildEventListener methods

    public void setOnChangedListener(OnChangedListener listener) {
        mListener = listener;
    }
    protected void notifyChangedListeners(OnChangedListener.EventType type, int index) {
        notifyChangedListeners(type, index, -1);
    }
    protected void notifyChangedListeners(OnChangedListener.EventType type, int index, int oldIndex) {
        if (mListener != null) {
            mListener.onChanged(type, index, oldIndex);
        }
    }

    // TODO protected?
    protected void reverse() {
        mOrder = Order.Descending;
        if (mComparator == null) {
            Collections.reverse(mSnapshots);
        } else {
            Collections.sort(mSnapshots, mComparator);
        }
        notifyChangedListeners(OnChangedListener.EventType.All, 0);
    }

    private Comparator<DataSnapshot> mComparator;
    private Predicate<DataSnapshot> mPredicate;
    private Order mOrder;

    protected void sortBy(final String key, final Order order, final Class<? extends Comparable> valueType) {
        mOrder = order;
        mComparator = new Comparator<DataSnapshot>() {
            @Override
            public int compare(DataSnapshot data1, DataSnapshot data2) {
                if (data1.hasChild(key) && data2.hasChild(key)) {
                    Comparable value1 = data1.child(key).getValue(valueType);
                    Comparable value2 = data2.child(key).getValue(valueType);
                    if (mOrder == Order.Ascending) {
                        return value1.compareTo(value2);
                    } else {
                        return value2.compareTo(value1);
                    }
                }
                System.err.println(
                        "Key " + key + " was not found in datasnapshot " + data1 + " or " + data2);
                return 0;
            }
        };
        Collections.sort(mSnapshots, mComparator);
        notifyChangedListeners(OnChangedListener.EventType.All, 0);
    }

    public void filter(Predicate<DataSnapshot> predicate) {
        // there's probably a one liner to do this... I'm looking at you, Guava
        mPredicate = predicate;
        for (int i = 0; i < mSnapshots.size(); i++) {
            if (predicate.evaluate(mSnapshots.get(i))) {
                mSnapshots.remove(i);
            }
        }
        notifyChangedListeners(OnChangedListener.EventType.All, 0);
    }
}
