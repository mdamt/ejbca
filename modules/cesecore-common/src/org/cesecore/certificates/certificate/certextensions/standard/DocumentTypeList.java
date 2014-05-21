/*************************************************************************
 *                                                                       *
 *  CESeCore: CE Security Core                                           *
 *                                                                       *
 *  This software is free software; you can redistribute it and/or       *
 *  modify it under the terms of the GNU Lesser General Public           *
 *  License as published by the Free Software Foundation; either         *
 *  version 2.1 of the License, or any later version.                    *
 *                                                                       *
 *  See terms of license at gnu.org.                                     *
 *                                                                       *
 *************************************************************************/ 
package org.cesecore.certificates.certificate.certextensions.standard;

import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1Object;
import org.bouncycastle.asn1.DERPrintableString;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.DERSet;
import org.bouncycastle.asn1.util.ASN1Dump;
import org.cesecore.certificates.ca.CA;
import org.cesecore.certificates.ca.internal.CertificateValidity;
import org.cesecore.certificates.certificateprofile.CertificateProfile;
import org.cesecore.certificates.endentity.EndEntityInformation;

/**
 * Class for standard X509 certificate extension. 
 * See rfc3280 or later for spec of this extension.
 * 
 * @version $Id$
 */
public class DocumentTypeList extends StandardCertificateExtension {
    private static final Logger log = Logger.getLogger(DocumentTypeList.class);

    @Override
    public void init(final CertificateProfile certProf) {
        super.setOID("2.23.136.1.1.6.2");
        super.setCriticalFlag(certProf.getDocumentTypeListCritical());
    }
    
    @Override
    public ASN1Encodable getValue(final EndEntityInformation subject, final CA ca, final CertificateProfile certProfile,
            final PublicKey userPublicKey, final PublicKey caPublicKey, CertificateValidity val) {
        
        ArrayList<String> docTypes = certProfile.getDocumentTypeList();
        if(docTypes.size() == 0) {
            if (log.isDebugEnabled()) {
                log.debug("No DocumentTypeList to make a certificate extension");
            }
            return null;
        }
        
        ASN1EncodableVector vec = new ASN1EncodableVector();

        // version
        vec.add(new ASN1Integer(0));
        
        // Add SET OF DocumentType
        Iterator<String> itr = docTypes.iterator();
        while(itr.hasNext()) {
            String type = itr.next();
            vec.add(new DERSet(new ASN1Encodable[]{new DERPrintableString(type)}));
        }
        
        ASN1Object gn = new DERSequence(vec);
        if(log.isDebugEnabled()) {
            log.debug("Constructed DocumentTypeList:");
            log.debug(ASN1Dump.dumpAsString(gn, true));
        }
        
        return gn;
    }   
}