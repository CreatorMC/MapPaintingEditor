package com.tmsstudio.mappaintingeditor.nbt.tags;

import com.tmsstudio.mappaintingeditor.nbt.convert.NBTConstants;

public class IntArrayTag extends Tag<int[]> {

    private static final long serialVersionUID = 827586678981022917L;

    public IntArrayTag(String name, int[] value) {
        super(name, value);
    }

    @Override
    public NBTConstants.NBTType getType() {
        return NBTConstants.NBTType.INT_ARRAY;
    }

    @Override
    public Tag<int[]> getDeepCopy() {
        return new IntArrayTag(name, value.clone());
    }

}
