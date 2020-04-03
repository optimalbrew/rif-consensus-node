package org.hyperledger.besu.ethereum.unitrie;

import com.google.common.base.Preconditions;

import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;

public abstract class EncodedUniNode extends AbstractUniNode {

    protected byte[] encoding;


    EncodedUniNode(final byte[] path, final ValueWrapper valueWrapper) {
        super(path,valueWrapper);
    }

    @Override
    public byte[] getEncoding() {
        return encoding;
    }

    @Override
    public boolean isDirty() {
        if (hasLongValue()) {
            return (longValueWrapper instanceof DirtyLongValueWrapper);
        } else
            return longValueWrapper==DirtyLongValueWrapper.DIRTY;
    }

    @Override
    public boolean hasLongValue() {
        return (longValueWrapper != null) && (longValueWrapper != DirtyLongValueWrapper.DIRTY);
    }
    @Override
    public void markDirty() {
        if (isDirty()) return;
        if (hasLongValue()) {
            longValueWrapper = new DirtyLongValueWrapper(
                    longValueWrapper.internalGetValue(),
                    longValueWrapper.internalGetHash(),
                    longValueWrapper.internalGetLength());
        }
        else
            longValueWrapper = DirtyLongValueWrapper.DIRTY;
    }

}
