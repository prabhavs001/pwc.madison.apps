package com.pwc.madison.core.models;

import org.osgi.annotation.versioning.ConsumerType;
import java.util.Calendar;


/**
 * Interface for a SME list item,
 */
@ConsumerType
public interface SmeListItem {

    /**
     * Returns the description of this {@code SmeListItem}.
     *
     * @return the description of this navigation item or {@code null}
     */
    default String getDescription() {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the date when this {@code SmeListItem} was last modified.
     *
     * @return the last modified date of this item or {@code null}
     */
    default Calendar getLastModified() {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the path of this {@code SmeListItem}.
     *
     * @return the list item path or {@code null}
     */
    default String getPath() {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the firstname of this {@code SmeListItem}.
     *
     * @return the firstname
     */
    default String getFirstname() {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the lastname of this {@code SmeListItem}.
     *
     * @return the lastname
     */
    default String getLastname() {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the stafflevel of this {@code SmeListItem}.
     *
     * @return the stafflevel
     */
    default String getStafflevel() { throw new UnsupportedOperationException(); }

    /**
     * Returns the organization of this {@code SmeListItem}.
     *
     * @return the organization
     */
    default String getOrganization() { throw new UnsupportedOperationException(); }

    /**
     * Returns the phone of this {@code SmeListItem}.
     *
     * @return the phone
     */
    default String getPhone() { throw new UnsupportedOperationException(); }

    /**
     * Returns the email of this {@code SmeListItem}.
     *
     * @return the email
     */
    default String getEmail() { throw new UnsupportedOperationException(); }

    /**
     * Returns the linkedin of this {@code SmeListItem}.
     *
     * @return the linkedin
     */
    default String getLinkedin() { throw new UnsupportedOperationException(); }

    /**
     * Returns the photo of this {@code SmeListItem}.
     *
     * @return the photo
     */
    default String getPhoto() { throw new UnsupportedOperationException(); }

    /**
     * Returns the biography of this {@code SmeListItem}.
     *
     * @return the biography
     */
    default String getBiography() { throw new UnsupportedOperationException(); }

    /**
     * Returns the first character of first name
     * @return the first character
     */
    default Character getFirstCharacterName(){throw new UnsupportedOperationException();}
}