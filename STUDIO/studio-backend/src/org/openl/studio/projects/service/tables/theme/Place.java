package org.openl.studio.projects.service.tables.theme;

import org.jspecify.annotations.Nullable;

import org.openl.studio.projects.model.tables.RawTableRole;

/**
 * What a place of a table is: the role it takes, how its value is typed, and the parts of a theme it takes its look
 * from when the theme is written.
 *
 * @param role   what the place means
 * @param typing how the value of the place is typed
 * @param parts  the parts of a theme the place takes its look from
 * @author Yury Molchan
 */
record Place(RawTableRole role, Typing typing, Parts parts) {

    /** How the value of a place is typed. */
    enum Typing {
        /** The place holds no value to type, such as a title or a name. */
        NONE,
        /** The value is typed as the compiler reads it. */
        VALUE,
        /** The value is code, whatever it reads like. */
        CODE
    }

    /**
     * A place of a role, its value typed as the role types it: the values, the outputs and the results are typed as
     * the compiler reads them, every other role holds no value to type.
     *
     * @param role  what the place means
     * @param parts the parts of a theme the place takes its look from
     * @return the place
     */
    static Place of(RawTableRole role, Parts parts) {
        return new Place(role, typingOf(role), parts);
    }

    /**
     * A place of a role holding code, such as the body of a Method table.
     *
     * @param role  what the place means
     * @param parts the parts of a theme the place takes its look from
     * @return the place
     */
    static Place code(RawTableRole role, Parts parts) {
        return new Place(role, Typing.CODE, parts);
    }

    /** This place with another part of a theme laid over its parts, or as it is for no part. */
    Place with(Parts.@Nullable Part part) {
        return part == null ? this : new Place(role, typing, parts.with(part));
    }

    /** This place with other parts. */
    Place withParts(Parts other) {
        return new Place(role, typing, other);
    }

    /** This place taking another role, its value typed as before unless the role holds no value. */
    Place as(RawTableRole other) {
        return new Place(other, typingOf(other) == Typing.NONE ? Typing.NONE : typing, parts);
    }

    /** This place holding no value to type, such as the name of a step a Spreadsheet returns. */
    Place untyped() {
        return new Place(role, Typing.NONE, parts);
    }

    private static Typing typingOf(RawTableRole role) {
        return switch (role) {
            case VALUE, OUTPUT, RESULT -> Typing.VALUE;
            default -> Typing.NONE;
        };
    }
}
