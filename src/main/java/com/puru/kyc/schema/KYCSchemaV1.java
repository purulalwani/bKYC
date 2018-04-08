package com.puru.kyc.schema;

import com.google.common.collect.ImmutableList;
import com.puru.kyc.model.KYC;
import net.corda.core.schemas.MappedSchema;
import net.corda.core.schemas.PersistentState;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.Date;
import java.util.UUID;

/**
 * An KYCState schema.
 */
public class KYCSchemaV1 extends MappedSchema {
    public KYCSchemaV1() {
        super(KYCSchema.class, 1, ImmutableList.of(PersistentKYC.class));
    }

    @Entity
    @Table(name = "kyc_states")
    public static class PersistentKYC extends PersistentState {
        @Column(name = "kyc_id") private final Integer kycId;
        @Column(name = "user_id") private final String userId;



        @Column(name = "user_name") private final String userName;
        @Column(name = "kyc_date") private final Date kycDate;
        @Column(name = "kyc_valid_date") private final Date kycValidDate;
        @Column(name = "doc_id") private final String docId;
        @Column(name = "buyer") private final String buyer;
        @Column(name = "seller") private final String seller;
        @Column(name = "linear_id") private final UUID linearId;


        public PersistentKYC(KYC kyc, String buyer, String seller, UUID linearId) {
            this.buyer = buyer;
            this.seller = seller;
            this.kycId = kyc.getKycId();
            this.userId = kyc.getUserId();
            this.userName = kyc.getUserName();
            this.kycDate = kyc.getKycDate();
            this.kycValidDate = kyc.getKycValidDate();
            this.docId = kyc.getDocId();
            this.linearId = linearId;
        }

        // Default constructor required by hibernate.
        public PersistentKYC() {
            this.buyer = null;
            this.seller = null;
            this.kycId = null;
            this.userId = null;
            this.userName = null;
            this.kycDate = null;
            this.kycValidDate = null;
            this.docId = null;
            this.linearId = null;
        }

        public String getUserId() {
            return userId;
        }

        public String getUserName() {
            return userName;
        }

        public Date getKycDate() {
            return kycDate;
        }

        public Date getKycValidDate() {
            return kycValidDate;
        }

        public String getDocId() {
            return docId;
        }

        public String getBuyer() {
            return buyer;
        }

        public String getSeller() {
            return seller;
        }

        public UUID getLinearId() {
            return linearId;
        }
    }
}