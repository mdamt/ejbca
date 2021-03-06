/*************************************************************************
 *                                                                       *
 *  EJBCA Community: The OpenSource Certificate Authority                *
 *                                                                       *
 *  This software is free software; you can redistribute it and/or       *
 *  modify it under the terms of the GNU Lesser General Public           *
 *  License as published by the Free Software Foundation; either         *
 *  version 2.1 of the License, or any later version.                    *
 *                                                                       *
 *  See terms of license at gnu.org.                                     *
 *                                                                       *
 *************************************************************************/
package org.ejbca.ra;

import java.io.Serializable;
import java.math.BigInteger;
import java.security.cert.Certificate;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TimeZone;

import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import javax.faces.component.UIComponent;
import javax.faces.component.html.HtmlOutputLabel;
import javax.faces.context.FacesContext;
import javax.faces.event.AjaxBehaviorEvent;
import javax.faces.model.SelectItem;

import org.apache.log4j.Logger;
import org.cesecore.certificates.ca.CAInfo;
import org.cesecore.certificates.certificate.CertificateConstants;
import org.cesecore.certificates.certificate.CertificateDataWrapper;
import org.cesecore.certificates.crl.RevokedCertInfo;
import org.cesecore.certificates.endentity.EndEntityInformation;
import org.cesecore.util.CertTools;
import org.cesecore.util.ValidityDate;
import org.ejbca.core.model.era.RaCertificateSearchRequest;
import org.ejbca.core.model.era.RaCertificateSearchResponse;
import org.ejbca.core.model.era.RaMasterApiProxyBeanLocal;

/**
 * Backing bean for Search Certificates page. 
 * 
 * @version $Id$
 */
@ManagedBean
@ViewScoped
public class RaSearchCertsBean implements Serializable {

    /** UI representation of a result set item from the back end. */
    public class RaSearchCertificate {
        private final String fingerprint;
        private final String username;
        private final String serialnumber;
        private final String serialnumberRaw;
        private final String subjectDn;
        private final String subjectAn;
        private final String eepName;
        private final String cpName;
        private final String caName;
        private final String created;
        private final String expires;
        private final int status;
        private final int revocationReason;
        private String updated;
        public RaSearchCertificate(final CertificateDataWrapper cdw) {
            this.fingerprint = cdw.getCertificateData().getFingerprint();
            this.serialnumberRaw = cdw.getCertificateData().getSerialNumber();
            this.serialnumber = new BigInteger(this.serialnumberRaw).toString(16);
            final String username = cdw.getCertificateData().getUsername();
            this.username = username==null ? "" : username;
            this.subjectDn = cdw.getCertificateData().getSubjectDN();
            final Certificate certificate = cdw.getCertificate();
            this.subjectAn = certificate==null ? "" : CertTools.getSubjectAlternativeName(certificate);
            final Integer cpId = cdw.getCertificateData().getCertificateProfileId();
            if (cpId != null && cpId.intValue()==EndEntityInformation.NO_CERTIFICATEPROFILE) {
                this.cpName = raLocaleBean.getMessage("search_certs_page_info_unknowncp");
            } else if (cpId != null && cpIdToNameMap!=null && cpIdToNameMap.containsKey(cpId)) {
                this.cpName = String.valueOf(cpIdToNameMap.get(cpId));
            } else {
                this.cpName = raLocaleBean.getMessage("search_certs_page_info_missingcp", cpId);
            }
            final int eepId = cdw.getCertificateData().getEndEntityProfileIdOrZero();
            if (eepId==EndEntityInformation.NO_ENDENTITYPROFILE) {
                this.eepName = raLocaleBean.getMessage("search_certs_page_info_unknowneep", eepId);
            } else if (eepIdToNameMap!=null && eepIdToNameMap.containsKey(Integer.valueOf(eepId))) {
                this.eepName = String.valueOf(eepIdToNameMap.get(Integer.valueOf(eepId)));
            } else {
                this.eepName = raLocaleBean.getMessage("search_certs_page_info_missingeep", eepId);
            }
            final String issuerDn = cdw.getCertificateData().getIssuerDN();
            if (issuerDn != null && caSubjectToNameMap.containsKey(issuerDn)) {
                this.caName = String.valueOf(caSubjectToNameMap.get(issuerDn));
            } else {
                this.caName = String.valueOf(issuerDn);
            }
            this.created = certificate==null ? "-" : ValidityDate.formatAsISO8601ServerTZ(CertTools.getNotBefore(certificate).getTime(), TimeZone.getDefault());
            this.expires = ValidityDate.formatAsISO8601ServerTZ(cdw.getCertificateData().getExpireDate(), TimeZone.getDefault());
            this.status = cdw.getCertificateData().getStatus();
            this.revocationReason = cdw.getCertificateData().getRevocationReason();
            if (status==CertificateConstants.CERT_ARCHIVED || status==CertificateConstants.CERT_REVOKED) {
                this.updated = ValidityDate.formatAsISO8601ServerTZ(cdw.getCertificateData().getRevocationDate(), TimeZone.getDefault());
            } else {
                this.updated = ValidityDate.formatAsISO8601ServerTZ(cdw.getCertificateData().getUpdateTime(), TimeZone.getDefault());
            }
        }
        public String getFingerprint() { return fingerprint; }
        public String getSerialnumber() { return serialnumber; }
        public String getSerialnumberRaw() { return serialnumberRaw; }
        public String getUsername() { return username; }
        public String getSubjectDn() { return subjectDn; }
        public String getSubjectAn() { return subjectAn; }
        public String getCaName() { return caName; }
        public String getCpName() { return cpName; }
        public boolean isCpNameSameAsEepName() { return eepName.equals(cpName); }
        public String getEepName() { return eepName; }
        public String getCreated() { return created; }
        public String getExpires() { return expires; }
        public boolean isActive() { return status==CertificateConstants.CERT_ACTIVE || status==CertificateConstants.CERT_NOTIFIEDABOUTEXPIRATION; }
        public String getStatus() {
            switch (status) {
            case CertificateConstants.CERT_ACTIVE:
            case CertificateConstants.CERT_NOTIFIEDABOUTEXPIRATION:
                return raLocaleBean.getMessage("search_certs_page_status_active");
            case CertificateConstants.CERT_ARCHIVED:
            case CertificateConstants.CERT_REVOKED:
                return raLocaleBean.getMessage("search_certs_page_status_revoked_"+revocationReason);
            }
            return raLocaleBean.getMessage("search_certs_page_status_other");
        }
        public String getUpdated() { return updated; }
    }
    
    private static final long serialVersionUID = 1L;
    private static final Logger log = Logger.getLogger(RaSearchCertsBean.class);

    @EJB
    private RaMasterApiProxyBeanLocal raMasterApiProxyBean;

    @ManagedProperty(value="#{raAuthenticationBean}")
    private RaAuthenticationBean raAuthenticationBean;
    public void setRaAuthenticationBean(final RaAuthenticationBean raAuthenticationBean) { this.raAuthenticationBean = raAuthenticationBean; }

    @ManagedProperty(value="#{raLocaleBean}")
    private RaLocaleBean raLocaleBean;
    public void setRaLocaleBean(final RaLocaleBean raLocaleBean) { this.raLocaleBean = raLocaleBean; }

    private final List<RaSearchCertificate> resultsFiltered = new ArrayList<>();
    private Map<Integer,String> eepIdToNameMap = null;
    private Map<Integer,String> cpIdToNameMap = null;
    private Map<String,String> caSubjectToNameMap = new HashMap<>();
    private List<SelectItem> availableEeps = new ArrayList<>();
    private List<SelectItem> availableCps = new ArrayList<>();
    private List<SelectItem> availableCas = new ArrayList<>();

    private RaCertificateSearchRequest stagedRequest = new RaCertificateSearchRequest();
    private RaCertificateSearchRequest lastExecutedRequest = null;
    private RaCertificateSearchResponse lastExecutedResponse = null;

    private String expiresAfter = "";
    private String expiresBefore = "";
    private String revokedAfter = "";
    private String revokedBefore = "";

    private enum SortOrder { PROFILE, CA, SERIALNUMBER, SUBJECT, USERNAME, EXPIRATION, STATUS };
    
    private SortOrder sortBy = SortOrder.USERNAME;
    private boolean sortAscending = true;

    private boolean moreOptions = false;

    /** Invoked action on search form post */
    public void searchAndFilterAction() {
        searchAndFilterCommon();
    }

    /** Invoked on criteria changes */
    public void searchAndFilterAjaxListener(final AjaxBehaviorEvent event) {
        searchAndFilterCommon();
    }
    
    /** Determine if we need to query back end or just filter and execute the required action. */
    private void searchAndFilterCommon() {
        final int compared = stagedRequest.compareTo(lastExecutedRequest);
        boolean search = compared>0;
        if (compared<=0 && lastExecutedResponse!=null) {
            // More narrow search → filter and check if there are sufficient results left
            if (log.isDebugEnabled()) {
                log.debug("More narrow criteria → Filter");
            }
            filterTransformSort();
            // Check if there are sufficient results to fill screen and search for more
            if (resultsFiltered.size()<lastExecutedRequest.getMaxResults() && lastExecutedResponse.isMightHaveMoreResults()) {
                if (log.isDebugEnabled()) {
                    log.debug("Trying to load more results since filter left too few results → Query");
                }
                search = true;
            } else {
                search = false;
            }
        }
        if (search) {
            // Wider search → Query back-end
            if (log.isDebugEnabled()) {
                log.debug("Wider criteria → Query");
            }
            lastExecutedResponse = raMasterApiProxyBean.searchForCertificates(raAuthenticationBean.getAuthenticationToken(), stagedRequest);
            lastExecutedRequest = stagedRequest;
            stagedRequest = new RaCertificateSearchRequest(stagedRequest);
            filterTransformSort();
        }
    }

    /** Perform in memory filtering using the current search criteria of the last result set from the back end. */
    private void filterTransformSort() {
        resultsFiltered.clear();
        if (lastExecutedResponse != null) {
            for (final CertificateDataWrapper cdw : lastExecutedResponse.getCdws()) {
                // ...we don't filter if the requested maxResults is lower than the search request
                if (!stagedRequest.getGenericSearchString().isEmpty() && (
                        (!cdw.getCertificateData().getSerialNumber().equals(stagedRequest.getGenericSearchStringAsDecimal())) &&
                        (!cdw.getCertificateData().getSerialNumber().equals(stagedRequest.getGenericSearchStringAsHex())) &&
                        (cdw.getCertificateData().getUsername() == null || !cdw.getCertificateData().getUsername().contains(stagedRequest.getGenericSearchString())) &&
                        (cdw.getCertificateData().getSubjectDN() == null || !cdw.getCertificateData().getSubjectDN().contains(stagedRequest.getGenericSearchString())))) {
                    continue;
                }
                if (!stagedRequest.getEepIds().isEmpty() && !stagedRequest.getEepIds().contains(cdw.getCertificateData().getEndEntityProfileIdOrZero())) {
                    continue;
                }
                if (!stagedRequest.getCpIds().isEmpty() && !stagedRequest.getCpIds().contains(cdw.getCertificateData().getCertificateProfileId())) {
                    continue;
                }
                if (!stagedRequest.getCaIds().isEmpty() && !stagedRequest.getCaIds().contains(cdw.getCertificateData().getIssuerDN().hashCode())) {
                    continue;
                }
                if (stagedRequest.getExpiresAfter()<Long.MAX_VALUE) {
                    if (cdw.getCertificateData().getExpireDate()<stagedRequest.getExpiresAfter()) {
                        continue;
                    }
                }
                if (stagedRequest.getExpiresBefore()>0L) {
                    if (cdw.getCertificateData().getExpireDate()>stagedRequest.getExpiresBefore()) {
                        continue;
                    }
                }
                if (stagedRequest.getRevokedAfter()<Long.MAX_VALUE) {
                    if (cdw.getCertificateData().getUpdateTime()<stagedRequest.getRevokedAfter()) {
                        continue;
                    }
                }
                if (stagedRequest.getRevokedBefore()>0L) {
                    if (cdw.getCertificateData().getUpdateTime()>stagedRequest.getRevokedBefore()) {
                        continue;
                    }
                }
                if (!stagedRequest.getStatuses().isEmpty() && !stagedRequest.getStatuses().contains(cdw.getCertificateData().getStatus())) {
                    continue;
                }
                if (!stagedRequest.getRevocationReasons().isEmpty() && !stagedRequest.getRevocationReasons().contains(cdw.getCertificateData().getRevocationReason())) {
                    continue;
                }
                resultsFiltered.add(new RaSearchCertificate(cdw));
            }
            if (log.isDebugEnabled()) {
                log.debug("Filtered " + lastExecutedResponse.getCdws().size() + " responses down to " + resultsFiltered.size() + " results.");
            }
            sort();
        }
    }

    /** Sort the filtered result set based on the select column and sort order. */
    private void sort() {
        Collections.sort(resultsFiltered, new Comparator<RaSearchCertificate>() {
            @Override
            public int compare(RaSearchCertificate o1, RaSearchCertificate o2) {
                switch (sortBy) {
                case PROFILE:
                    return o1.eepName.concat(o1.cpName).compareTo(o2.eepName.concat(o2.cpName)) * (sortAscending ? 1 : -1);
                case CA:
                    return o1.caName.compareTo(o2.caName) * (sortAscending ? 1 : -1);
                case SERIALNUMBER:
                    return o1.serialnumber.compareTo(o2.serialnumber) * (sortAscending ? 1 : -1);
                case SUBJECT:
                    return (o1.subjectDn+o1.subjectAn).compareTo(o2.subjectDn+o2.subjectAn) * (sortAscending ? 1 : -1);
                case EXPIRATION:
                    return o1.expires.compareTo(o2.expires) * (sortAscending ? 1 : -1);
                case STATUS:
                    return o1.getStatus().compareTo(o2.getStatus()) * (sortAscending ? 1 : -1);
                case USERNAME:
                default:
                    return o1.username.compareTo(o2.username) * (sortAscending ? 1 : -1);
                }
            }
        });
    }
    
    public String getSortedByProfile() { return getSortedBy(SortOrder.PROFILE); }
    public void sortByProfile() { sortBy(SortOrder.PROFILE, true); }
    public String getSortedByCa() { return getSortedBy(SortOrder.CA); }
    public void sortByCa() { sortBy(SortOrder.CA, true); }
    public String getSortedBySerialNumber() { return getSortedBy(SortOrder.SERIALNUMBER); }
    public void sortBySerialNumber() { sortBy(SortOrder.SERIALNUMBER, true); }
    public String getSortedBySubject() { return getSortedBy(SortOrder.SUBJECT); }
    public void sortBySubject() { sortBy(SortOrder.SUBJECT, true); }
    public String getSortedByExpiration() { return getSortedBy(SortOrder.EXPIRATION); }
    public void sortByExpiration() { sortBy(SortOrder.EXPIRATION, false); }
    public String getSortedByStatus() { return getSortedBy(SortOrder.STATUS); }
    public void sortByStatus() { sortBy(SortOrder.STATUS, true); }
    public String getSortedByUsername() { return getSortedBy(SortOrder.USERNAME); }
    public void sortByUsername() { sortBy(SortOrder.USERNAME, true); }

    /** @return an up or down arrow character depending on sort order if the sort column matches */
    private String getSortedBy(final SortOrder sortOrder) {
        if (sortBy.equals(sortOrder)) {
            return sortAscending ? "\u25bc" : "\u25b2";
        }
        return "";
    }
    /** Set current sort column. Flip the order if the column was already selected. */
    private void sortBy(final SortOrder sortOrder, final boolean defaultAscending) {
        if (sortBy.equals(sortOrder)) {
            sortAscending = !sortAscending;
        } else {
            sortAscending = defaultAscending;
        }
        this.sortBy = sortOrder;
        sort();
    }
    
    /** @return true if there might be more results in the back end than retrieved based on the current criteria. */
    public boolean isMoreResultsAvailable() {
        return lastExecutedResponse!=null && lastExecutedResponse.isMightHaveMoreResults();
    }

    /** @return true of more search criteria than just the basics should be shown */
    public boolean isMoreOptions() { return moreOptions; };

    /** Invoked when more or less options action is invoked. */
    public void moreOptionsAction() {
        moreOptions = !moreOptions;
        // Reset any criteria in the advanced section
        stagedRequest.setMaxResults(RaCertificateSearchRequest.DEFAULT_MAX_RESULTS);
        stagedRequest.setExpiresAfter(Long.MAX_VALUE);
        stagedRequest.setExpiresBefore(0L);
        stagedRequest.setRevokedAfter(Long.MAX_VALUE);
        stagedRequest.setRevokedBefore(0L);
        expiresAfter = "";
        expiresBefore = "";
        revokedAfter = "";
        revokedBefore = "";
        searchAndFilterCommon();
    }

    public List<RaSearchCertificate> getFilteredResults() {
        return resultsFiltered;
    }

    public String getGenericSearchString() { return stagedRequest.getGenericSearchString(); }
    public void setGenericSearchString(final String genericSearchString) { stagedRequest.setGenericSearchString(genericSearchString); }
    
    public int getCriteriaMaxResults() { return stagedRequest.getMaxResults(); }
    public void setCriteriaMaxResults(final int criteriaMaxResults) { stagedRequest.setMaxResults(criteriaMaxResults); }
    public List<SelectItem> getAvailableMaxResults() {
        List<SelectItem> ret = new ArrayList<>();
        for (final int value : new int[]{ RaCertificateSearchRequest.DEFAULT_MAX_RESULTS, 50, 100, 200, 400}) {
            ret.add(new SelectItem(value, raLocaleBean.getMessage("search_certs_page_criteria_results_option", value)));
        }
        return ret;
    }

    public int getCriteriaEepId() {
        return stagedRequest.getEepIds().isEmpty() ? 0 : stagedRequest.getEepIds().get(0);
    }
    public void setCriteriaEepId(final int criteriaEepId) {
        if (criteriaEepId==0) {
            stagedRequest.setEepIds(new ArrayList<Integer>());
        } else {
            stagedRequest.setEepIds(new ArrayList<>(Arrays.asList(new Integer[]{ criteriaEepId })));
        }
    }
    public boolean isOnlyOneEepAvailable() { return getAvailableEeps().size()==1; }
    public List<SelectItem> getAvailableEeps() {
        if (availableEeps.isEmpty()) {
            eepIdToNameMap = raMasterApiProxyBean.getAuthorizedEndEntityProfileIdsToNameMap(raAuthenticationBean.getAuthenticationToken());
            availableEeps.add(new SelectItem(0, raLocaleBean.getMessage("search_certs_page_criteria_eep_optionany")));
            for (final Entry<Integer,String> entry : getAsSortedByValue(eepIdToNameMap.entrySet())) {
                availableEeps.add(new SelectItem(entry.getKey(), "- " + entry.getValue()));
            }
        }
        return availableEeps;
    }

    public int getCriteriaCpId() {
        return stagedRequest.getCpIds().isEmpty() ? 0 : stagedRequest.getCpIds().get(0);
    }
    public void setCriteriaCpId(final int criteriaCpId) {
        if (criteriaCpId==0) {
            stagedRequest.setCpIds(new ArrayList<Integer>());
        } else {
            stagedRequest.setCpIds(new ArrayList<>(Arrays.asList(new Integer[]{ criteriaCpId })));
        }
    }
    public boolean isOnlyOneCpAvailable() { return getAvailableCps().size()==1; }
    public List<SelectItem> getAvailableCps() {
        if (availableCps.isEmpty()) {
            cpIdToNameMap = raMasterApiProxyBean.getAuthorizedCertificateProfileIdsToNameMap(raAuthenticationBean.getAuthenticationToken());
            availableCps.add(new SelectItem(0, raLocaleBean.getMessage("search_certs_page_criteria_cp_optionany")));
            for (final Entry<Integer,String> entry : getAsSortedByValue(cpIdToNameMap.entrySet())) {
                availableCps.add(new SelectItem(entry.getKey(), "- " + entry.getValue()));
            }
        }
        return availableCps;
    }

    public int getCriteriaCaId() {
        return stagedRequest.getCaIds().isEmpty() ? 0 : stagedRequest.getCaIds().get(0);
    }
    public void setCriteriaCaId(int criteriaCaId) {
        if (criteriaCaId==0) {
            stagedRequest.setCaIds(new ArrayList<Integer>());
        } else {
            stagedRequest.setCaIds(new ArrayList<>(Arrays.asList(new Integer[]{ criteriaCaId })));
        }
    }
    public boolean isOnlyOneCaAvailable() { return getAvailableCas().size()==1; }
    public List<SelectItem> getAvailableCas() {
        if (availableCas.isEmpty()) {
            final List<CAInfo> caInfos = new ArrayList<>(raMasterApiProxyBean.getAuthorizedCas(raAuthenticationBean.getAuthenticationToken()));
            Collections.sort(caInfos, new Comparator<CAInfo>() {
                @Override
                public int compare(final CAInfo caInfo1, final CAInfo caInfo2) {
                    return caInfo1.getName().compareTo(caInfo2.getName());
                }
            });
            for (final CAInfo caInfo : caInfos) {
                caSubjectToNameMap.put(caInfo.getSubjectDN(), caInfo.getName());
            }
            availableCas.add(new SelectItem(0, raLocaleBean.getMessage("search_certs_page_criteria_ca_optionany")));
            for (final CAInfo caInfo : caInfos) {
                availableCas.add(new SelectItem(caInfo.getCAId(), "- " + caInfo.getName()));
            }
        }
        return availableCas;
    }

    public String getExpiresAfter() {
        return getDateAsString(expiresAfter, stagedRequest.getExpiresAfter(), Long.MAX_VALUE);
    }
    public void setExpiresAfter(final String expiresAfter) {
        this.expiresAfter = expiresAfter;
        stagedRequest.setExpiresAfter(parseDateAndUseDefaultOnFail(expiresAfter, Long.MAX_VALUE));
    }
    public String getExpiresBefore() {
        return getDateAsString(expiresBefore, stagedRequest.getExpiresBefore(), 0L);
    }
    public void setExpiresBefore(final String expiresBefore) {
        this.expiresBefore = expiresBefore;
        stagedRequest.setExpiresBefore(parseDateAndUseDefaultOnFail(expiresBefore, 0L));
    }
    public String getRevokedAfter() {
        return getDateAsString(revokedAfter, stagedRequest.getRevokedAfter(), Long.MAX_VALUE);
    }
    public void setRevokedAfter(final String revokedAfter) {
        this.revokedAfter = revokedAfter;
        stagedRequest.setRevokedAfter(parseDateAndUseDefaultOnFail(revokedAfter, Long.MAX_VALUE));
    }
    public String getRevokedBefore() {
        return getDateAsString(revokedBefore, stagedRequest.getRevokedBefore(), 0L);
    }
    public void setRevokedBefore(final String revokedBefore) {
        this.revokedBefore = revokedBefore;
        stagedRequest.setRevokedBefore(parseDateAndUseDefaultOnFail(revokedBefore, 0L));
    }

    /** @return the current value if the staged request value if the default value */
    private String getDateAsString(final String stagedValue, final long value, final long defaultValue) {
        if (value==defaultValue) {
            return stagedValue;
        }
        return ValidityDate.formatAsISO8601ServerTZ(value, TimeZone.getDefault());
    }
    /** @return the staged request value if it is a parsable date and the default value otherwise */
    private long parseDateAndUseDefaultOnFail(final String input, final long defaultValue) {
        markCurrentComponentAsValid(true);
        if (!input.trim().isEmpty()) {
            try {
                return ValidityDate.parseAsIso8601(input).getTime();
            } catch (ParseException e) {
                markCurrentComponentAsValid(false);
                raLocaleBean.addMessageWarn("search_certs_page_warn_invaliddate");
            }
        }
        return defaultValue;
    }
    
    /** Set or remove the styleClass "invalidInput" on the label with a for-attribute matching the current input component. */
    private void markCurrentComponentAsValid(final boolean valid) {
        final String STYLE_CLASS_INVALID = "invalidInput";
        // UIComponent.getCurrentComponent only works when invoked via f:ajax
        final UIComponent uiComponent = UIComponent.getCurrentComponent(FacesContext.getCurrentInstance());
        final String id = uiComponent.getId();
        final List<UIComponent> siblings = uiComponent.getParent().getChildren();
        for (final UIComponent sibling : siblings) {
            if (sibling instanceof HtmlOutputLabel) {
                final HtmlOutputLabel htmlOutputLabel = (HtmlOutputLabel) sibling;
                if (htmlOutputLabel.getFor().equals(id)) {
                    String styleClass = htmlOutputLabel.getStyleClass();
                    if (valid) {
                        if (styleClass!=null && styleClass.contains(STYLE_CLASS_INVALID)) {
                            styleClass = styleClass.replace(STYLE_CLASS_INVALID, "").trim();
                        }
                    } else {
                        if (styleClass==null) {
                            styleClass = STYLE_CLASS_INVALID;
                        } else {
                            if (!styleClass.contains(STYLE_CLASS_INVALID)) {
                                styleClass = styleClass.concat(" " + STYLE_CLASS_INVALID);
                            }
                        }
                    }
                    htmlOutputLabel.setStyleClass(styleClass);
                }
            }
        }
    }

    public String getCriteriaStatus() {
        final StringBuilder sb = new StringBuilder();
        final List<Integer> statuses = stagedRequest.getStatuses();
        final List<Integer> revocationReasons = stagedRequest.getRevocationReasons();
        if (statuses.contains(CertificateConstants.CERT_ACTIVE)) {
            sb.append(CertificateConstants.CERT_ACTIVE);
        } else if (statuses.contains(CertificateConstants.CERT_REVOKED)) {
            sb.append(CertificateConstants.CERT_REVOKED);
            if (!revocationReasons.isEmpty()) {
                sb.append("_").append(revocationReasons.get(0));
            }
        }
        return sb.toString();
    }
    public void setCriteriaStatus(final String criteriaStatus) {
        final List<Integer> statuses = new ArrayList<>();
        final List<Integer> revocationReasons = new ArrayList<>();
        if (!criteriaStatus.isEmpty()) {
            final String[] criteriaStatusSplit = criteriaStatus.split("_");
            if (String.valueOf(CertificateConstants.CERT_ACTIVE).equals(criteriaStatusSplit[0])) {
                statuses.addAll(Arrays.asList(new Integer[]{ CertificateConstants.CERT_ACTIVE, CertificateConstants.CERT_NOTIFIEDABOUTEXPIRATION }));
            } else {
                statuses.addAll(Arrays.asList(new Integer[]{ CertificateConstants.CERT_REVOKED, CertificateConstants.CERT_ARCHIVED }));
                if (criteriaStatusSplit.length>1) {
                    revocationReasons.addAll(Arrays.asList(new Integer[]{ Integer.parseInt(criteriaStatusSplit[1]) }));
                }
            }
        }
        stagedRequest.setStatuses(statuses);
        stagedRequest.setRevocationReasons(revocationReasons);
    }
    
    public List<SelectItem> getAvailableStatuses() {
        final List<SelectItem> ret = new ArrayList<>();
        ret.add(new SelectItem("", raLocaleBean.getMessage("search_certs_page_criteria_status_option_any")));
        ret.add(new SelectItem(String.valueOf(CertificateConstants.CERT_ACTIVE), raLocaleBean.getMessage("search_certs_page_criteria_status_option_active")));
        ret.add(new SelectItem(String.valueOf(CertificateConstants.CERT_REVOKED), raLocaleBean.getMessage("search_certs_page_criteria_status_option_revoked")));
        ret.add(getAvailableStatusRevoked(RevokedCertInfo.REVOCATION_REASON_UNSPECIFIED));
        ret.add(getAvailableStatusRevoked(RevokedCertInfo.REVOCATION_REASON_KEYCOMPROMISE));
        ret.add(getAvailableStatusRevoked(RevokedCertInfo.REVOCATION_REASON_CACOMPROMISE));
        ret.add(getAvailableStatusRevoked(RevokedCertInfo.REVOCATION_REASON_AFFILIATIONCHANGED));
        ret.add(getAvailableStatusRevoked(RevokedCertInfo.REVOCATION_REASON_SUPERSEDED));
        ret.add(getAvailableStatusRevoked(RevokedCertInfo.REVOCATION_REASON_CESSATIONOFOPERATION));
        ret.add(getAvailableStatusRevoked(RevokedCertInfo.REVOCATION_REASON_CERTIFICATEHOLD));
        ret.add(getAvailableStatusRevoked(RevokedCertInfo.REVOCATION_REASON_REMOVEFROMCRL));
        ret.add(getAvailableStatusRevoked(RevokedCertInfo.REVOCATION_REASON_PRIVILEGESWITHDRAWN));
        ret.add(getAvailableStatusRevoked(RevokedCertInfo.REVOCATION_REASON_AACOMPROMISE));
        return ret;
    }
    private SelectItem getAvailableStatusRevoked(final int reason) {
        return new SelectItem(CertificateConstants.CERT_REVOKED + "_" + reason, raLocaleBean.getMessage("search_certs_page_criteria_status_option_revoked_reason_"+reason));
    }

    private <T> List<Entry<T, String>> getAsSortedByValue(final Set<Entry<T, String>> entrySet) {
        final List<Entry<T, String>> entrySetSorted = new ArrayList<>(entrySet);
        Collections.sort(entrySetSorted, new Comparator<Entry<T, String>>() {
            @Override
            public int compare(final Entry<T, String> o1, final Entry<T, String> o2) {
                return o1.getValue().compareTo(o2.getValue());
            }
        });
        return entrySetSorted;
    }
}
