package org.openas2.lib.dbUtils;

import org.joda.time.DateTime;

public class Profile {

    public static final String PROFILENAME = "serverProfile";

    public String getId() {
        return _id;
    }
    public void setId(String id) {
        this._id = id;
    }
    public String getAS2Idenitfier() {
        return _as2Idenitfier;
    }
    public void setAS2Idenitfier(String as2Idenitfier) {
        this._as2Idenitfier = as2Idenitfier;
    }
    public String getEmailAddress() {
        return _emailAddress;
    }
    public void setEemailAddress(String emailAddress) {
        this._emailAddress = emailAddress;
    }
    public String getAsynchronousMDNURL() {
        return _asynchronousMDNURL;
    }
    public void setAsynchronousMDNURL(String asynchronousMDNURL) {
        this._asynchronousMDNURL = asynchronousMDNURL;
    }
    public String getCertificatePassword() {
        return _certificatePassword;
    }
    public void setCertificatePassword(String certificatePassword) {
        this._certificatePassword = certificatePassword;
    }
    public String getPublicCertificate() {
        return _publicCertificate;
    }
    public void setPublicCertificate(String publicCertificate) {
        this._publicCertificate = publicCertificate;
    }
    public String getPrivateCertificate() {
        return _privateCertificate;
    }
    public void setPrivateCertificate(String privateCertificate) {
        this._privateCertificate = privateCertificate;
    }
    public String getUpdatedBy() {
        return _updatedBy;
    }
    public void setUpdatedBy(String updatedBy) {
        this._updatedBy = updatedBy;
    }
    public String getCreatedBy() {
        return _createdBy;
    }
    public void setCreatedBy(String createdBy) {
        this._createdBy = createdBy;
    }
    public DateTime getCreatedOn() {
        return _createdOn;
    }
    public void setCreatedOn(DateTime createdOn) {
        this._createdOn = createdOn;
    }
    public DateTime getUpdatedOn() {
        return _updatedOn;
    }
    public void setUpdatedOn(DateTime updatedOn) {
        this._updatedOn = updatedOn;
    }
    public boolean getIsMainProfile() {  return _isMainProfile;   }
    public String getDescription() {  return _description; }
    public String getOnPremHomeDirectory() {  return _onPremHomeDirectory; }
    public String getOnPremHomeDirectoryLinux() { return _onPremHomeDirectoryLinux; }
    public String getDataFolder() {  return _dataFolder;  }
    public void setIsMainProfile(boolean isMainProfile) {  this._isMainProfile = isMainProfile;  }
    public void setDescription(String description) { this._description = description;  }
    public void setOnPremHomeDirectory(String onPremHomeDirectory) { this._onPremHomeDirectory = onPremHomeDirectory;}
    public void setOnPremHomeDirectoryLinux(String onPremHomeDirectoryLinux) {  this._onPremHomeDirectoryLinux = onPremHomeDirectoryLinux; }
    public void setDataFolder(String dataFolder) {  this._dataFolder = dataFolder;  }



    private String _id;
    private String _as2Idenitfier;
    private String _emailAddress;
    private String _asynchronousMDNURL;
    private String _certificatePassword;
    private String _publicCertificate;
    private String _privateCertificate;
    private String _updatedBy;
    private String _createdBy;
    private DateTime _createdOn;
    private DateTime _updatedOn;
    private String _description;
    private boolean _isMainProfile;
    private String _onPremHomeDirectory;
    private String _onPremHomeDirectoryLinux;
    private String _dataFolder;


}
