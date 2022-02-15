package org.eclipse.dataspaceconnector.dataplane.azurestorage.adapter;

import java.io.InputStream;
import java.io.OutputStream;

public interface BlobAdapter {
    OutputStream getOutputStream();

    InputStream openInputStream();

    String getBlobName();

    long getBlobSize();
}
