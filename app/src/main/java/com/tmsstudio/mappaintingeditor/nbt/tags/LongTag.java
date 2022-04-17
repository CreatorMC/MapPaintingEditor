package com.tmsstudio.mappaintingeditor.nbt.tags;

import com.tmsstudio.mappaintingeditor.nbt.convert.NBTConstants;

public class LongTag extends Tag<Long> {

    private static final long serialVersionUID = 1571527153983268515L;

    public LongTag(String name, long value) {
        super(name, value);
    }

    @Override
    public NBTConstants.NBTType getType() {
        return NBTConstants.NBTType.LONG;
    }

    @Override
    public LongTag getDeepCopy() {
        return new LongTag(name, value);
    }
}