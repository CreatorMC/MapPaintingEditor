package com.tmsstudio.mappaintingeditor.nbt.tags;

import com.tmsstudio.mappaintingeditor.nbt.convert.NBTConstants;

public class ShortTag extends Tag<Short> {

    private static final long serialVersionUID = 8478629669505321780L;

    public ShortTag(String name, short value) {
        super(name, value);
    }

    @Override
    public NBTConstants.NBTType getType() {
        return NBTConstants.NBTType.SHORT;
    }


    @Override
    public ShortTag getDeepCopy() {
        return new ShortTag(name, value);
    }
}