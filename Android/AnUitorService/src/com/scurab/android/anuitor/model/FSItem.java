package com.scurab.android.anuitor.model;

import com.google.gson.annotations.SerializedName;

/**
 * User: jbruchanov
 * Date: 15/05/2014
 * Time: 14:13
 */
public class FSItem implements Comparable<FSItem> {

    public static int TYPE_PARENT_FOLDER = -1;
    public static int TYPE_FILE = 1;
    public static int TYPE_FOLDER = 2;

    @SerializedName("Name")
    private String mName;

    @SerializedName("Size")
    private long mSize;

    @SerializedName("Type")
    private int mType;

    public FSItem(String name, long size, int type) {
        mName = name;
        mSize = size;
        mType = type;
    }

    public String getName() {
        return mName;
    }

    public long getSize() {
        return mSize;
    }

    public int getType() {
        return mType;
    }

    @Override
    public int compareTo(FSItem o) {
        if (mType == o.mType) {
            return mName.compareTo(o.mName);
        } else {
            return mType - o.mType;
        }
    }
}
