package ru.bssg.lottabyte.core.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import ru.bssg.lottabyte.core.util.CoreUtils;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Data
@NoArgsConstructor
public abstract class AbstractPaginatedList<T> {

    @Schema(
            description = "Results of the list / search."
    )
    private List<T> resources;

    @Schema(
            description = "The limit parameter passed to the list / search method."
    )
    private int limit;

    @Schema(
            description = "The total number of items that matches the given criteria."
    )
    private long count;

    @Schema(
            name = "CoreResourceLink",
            description = "Represents an html link."
    )
    @Data
    @ToString
    public static class Link {

        @Schema(
                description = "URL to access the given Glossary resource"
        )
        private final String href;

        @Schema(
                description = "The offset of given Glossary resource"
        )
        private final long offset;

        public Link(URI uri, long offset) {
            this.href = CoreUtils.getRelativeHref(uri);
            this.offset = offset;
        }

        public boolean equals(Object other) {
            if (other == this) {
                return true;
            } else if (!(other instanceof AbstractPaginatedList.Link)) {
                return false;
            } else {
                AbstractPaginatedList.Link otherLink = (AbstractPaginatedList.Link)other;
                return Objects.equals(this.href, otherLink.href) && this.offset == otherLink.offset;
            }
        }

        public int hashCode() {
            return Objects.hash(new Object[]{this.href, this.offset});
        }
    }

}
