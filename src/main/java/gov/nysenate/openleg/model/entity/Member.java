package gov.nysenate.openleg.model.entity;

import java.io.Serializable;

public class Member extends Person implements Serializable
{
    private static final long serialVersionUID = -8348372884270872363L;

    /** Unique member id generated by the persistence layer. */
    private int memberId;

    /** Current mapping to LBDC's representation of the member id.
     *  This shortName is only unique to the scope of a (2 year) session */
    private String lbdcShortName;

    /** The session year the member is active in. */
    private int sessionYear;

    /** The legislative chamber this member is associated with. */
    private Chamber chamber;

    /** Indicates if the member is currently an incumbent. */
    private boolean incumbent;

    /** The district number the member is serving in during the given session year. */
    private Integer districtCode;

    /** --- Constructors --- */

    public Member() {}

    public Member(Member other) {
        super(other);
        this.memberId = other.memberId;
        this.lbdcShortName = other.lbdcShortName;
        this.sessionYear = other.sessionYear;
        this.chamber = other.chamber;
        this.incumbent = other.incumbent;
        this.districtCode = other.districtCode;
    }

    /** --- Overrides --- */

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Member)) return false;
        if (!super.equals(o)) return false;
        Member member = (Member) o;
        if (incumbent != member.incumbent) return false;
        if (memberId != member.memberId) return false;
        if (sessionYear != member.sessionYear) return false;
        if (chamber != member.chamber) return false;
        if (districtCode != null ? !districtCode.equals(member.districtCode) : member.districtCode != null)
            return false;
        if (lbdcShortName != null ? !lbdcShortName.equals(member.lbdcShortName) : member.lbdcShortName != null)
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + memberId;
        result = 31 * result + (lbdcShortName != null ? lbdcShortName.hashCode() : 0);
        result = 31 * result + sessionYear;
        result = 31 * result + (chamber != null ? chamber.hashCode() : 0);
        result = 31 * result + (incumbent ? 1 : 0);
        result = 31 * result + (districtCode != null ? districtCode.hashCode() : 0);
        return result;
    }

    /** --- Basic Getters/Setters --- */

    public int getMemberId() {
        return memberId;
    }

    public void setMemberId(int memberId) {
        this.memberId = memberId;
    }

    public Chamber getChamber() {
        return chamber;
    }

    public void setChamber(Chamber chamber) {
        this.chamber = chamber;
    }

    public boolean isIncumbent() {
        return incumbent;
    }

    public void setIncumbent(boolean incumbent) {
        this.incumbent = incumbent;
    }

    public String getLbdcShortName() {
        return lbdcShortName;
    }

    public void setLbdcShortName(String lbdcShortName) {
        this.lbdcShortName = lbdcShortName;
    }

    public int getSessionYear() {
        return sessionYear;
    }

    public void setSessionYear(int sessionYear) {
        this.sessionYear = sessionYear;
    }

    public Integer getDistrictCode() {
        return districtCode;
    }

    public void setDistrictCode(Integer districtCode) {
        this.districtCode = districtCode;
    }
}