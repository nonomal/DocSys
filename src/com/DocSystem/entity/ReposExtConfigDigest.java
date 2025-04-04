package com.DocSystem.entity;

public class ReposExtConfigDigest {
	public final static String RemoteStorageConfig = "RemoteStorageConfig";
    public final static String RemoteServerConfig = "RemoteServerConfig";
    public final static String AutoSyncupConfig = "AutoSyncupConfig";
    public final static String AutoBackupConfig = "AutoBackupConfig";
    public final static String TextSearchConfig = "TextSearchConfig";
    public final static String RecycleBinConfig = "RcycleBinConfig";
    public final static String VersionIgnoreConfig = "VersionIgnoreConfig";
    public final static String EncryptConfig = "EncryptConfig";

	public Integer reposId;
	public String remoteStorageConfigCheckSum;
	public String remoteServerConfigCheckSum;
	public String autoSyncupConfigCheckSum;
	public String autoBackupConfigCheckSum;
	public String textSearchConfigCheckSum;
	public String recycleBinConfigCheckSum;
	public String versionIgnoreConfigCheckSum;
	public String encryptConfigCheckSum;
}
