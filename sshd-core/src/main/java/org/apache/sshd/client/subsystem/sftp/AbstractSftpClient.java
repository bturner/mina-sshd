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

package org.apache.sshd.client.subsystem.sftp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.sshd.client.subsystem.sftp.extensions.BuiltinSftpClientExtensions;
import org.apache.sshd.client.subsystem.sftp.extensions.SftpClientExtension;
import org.apache.sshd.client.subsystem.sftp.extensions.SftpClientExtensionFactory;
import org.apache.sshd.common.subsystem.sftp.SftpConstants;
import org.apache.sshd.common.subsystem.sftp.extensions.ParserUtils;
import org.apache.sshd.common.util.GenericUtils;
import org.apache.sshd.common.util.logging.AbstractLoggingBean;

/**
 * @author <a href="mailto:dev@mina.apache.org">Apache MINA SSHD Project</a>
 */
public abstract class AbstractSftpClient extends AbstractLoggingBean implements SftpClient, RawSftpClient {
    private final AtomicReference<Map<String,Object>> parsedExtensionsHolder = new AtomicReference<Map<String,Object>>(null);

    protected AbstractSftpClient() {
        super();
    }
    
    @Override
    public String getName() {
        return SftpConstants.SFTP_SUBSYSTEM_NAME;
    }

    @Override
    public CloseableHandle open(String path) throws IOException {
        return open(path, Collections.<OpenMode>emptySet());
    }
    
    @Override
    public CloseableHandle open(String path, OpenMode ... options) throws IOException {
        return open(path, GenericUtils.of(options));
    }

    @Override
    public void rename(String oldPath, String newPath) throws IOException {
        rename(oldPath, newPath, Collections.<CopyMode>emptySet());
    }
    
    @Override
    public void rename(String oldPath, String newPath, CopyMode ... options) throws IOException {
        rename(oldPath, newPath, GenericUtils.of(options));
    }

    @Override
    public InputStream read(String path) throws IOException {
        return read(path, DEFAULT_READ_BUFFER_SIZE);
    }

    @Override
    public InputStream read(String path, int bufferSize) throws IOException {
        return read(path, bufferSize, EnumSet.of(OpenMode.Read));
    }

    @Override
    public InputStream read(String path, OpenMode ... mode) throws IOException {
        return read(path, DEFAULT_READ_BUFFER_SIZE, mode);
    }

    @Override
    public InputStream read(String path, int bufferSize, OpenMode ... mode) throws IOException {
        return read(path, bufferSize, GenericUtils.of(mode));
    }

    @Override
    public InputStream read(String path, Collection<OpenMode>  mode) throws IOException {
        return read(path, DEFAULT_READ_BUFFER_SIZE, mode);
    }

    @Override
    public int read(Handle handle, long fileOffset, byte[] dst) throws IOException {
        return read(handle, fileOffset, dst, 0, dst.length);
    }

    @Override
    public OutputStream write(String path) throws IOException {
        return write(path, DEFAULT_WRITE_BUFFER_SIZE);
    }

    @Override
    public OutputStream write(String path, int bufferSize) throws IOException {
        return write(path, bufferSize, EnumSet.of(OpenMode.Write, OpenMode.Create, OpenMode.Truncate));
    }

    @Override
    public OutputStream write(String path, OpenMode ... mode) throws IOException {
        return write(path, DEFAULT_WRITE_BUFFER_SIZE, mode);
    }

    @Override
    public OutputStream write(String path, Collection<OpenMode> mode) throws IOException {
        return write(path, DEFAULT_WRITE_BUFFER_SIZE, mode);
    }

    @Override
    public OutputStream write(String path, int bufferSize, OpenMode ... mode) throws IOException {
        return write(path, bufferSize, GenericUtils.of(mode));
    }

    @Override
    public void write(Handle handle, long fileOffset, byte[] src) throws IOException {
        write(handle, fileOffset, src, 0, src.length);
    }

    @Override
    public void symLink(String linkPath, String targetPath) throws IOException {
        link(linkPath, targetPath, true);
    }

    @Override
    public <E extends SftpClientExtension> E getExtension(Class<? extends E> extensionType) {
        Object  instance = getExtension(BuiltinSftpClientExtensions.fromType(extensionType));
        if (instance == null) {
            return null;
        } else {
            return extensionType.cast(instance);
        }
    }

    @Override
    public SftpClientExtension getExtension(String extensionName) {
        return getExtension(BuiltinSftpClientExtensions.fromName(extensionName));
    }
    
    protected SftpClientExtension getExtension(SftpClientExtensionFactory factory) {
        if (factory == null) {
            return null;
        }

        Map<String,byte[]> extensions = getServerExtensions();
        Map<String,Object> parsed = parsedExtensionsHolder.get();
        if (parsed == null) {
            if ((parsed=ParserUtils.parse(extensions)) == null) {
                parsed = Collections.<String,Object>emptyMap();
            }
            parsedExtensionsHolder.set(parsed);
        }

        return factory.create(this, this, extensions, parsed);
    }
}
