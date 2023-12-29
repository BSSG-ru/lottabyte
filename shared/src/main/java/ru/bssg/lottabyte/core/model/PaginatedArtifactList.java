package ru.bssg.lottabyte.core.model;

import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

@Slf4j
public class PaginatedArtifactList<RMOT extends ModeledObject<? extends Entity>> extends AbstractOffsetPaginatedList<RMOT> {

    public PaginatedArtifactList() {
    }

    public PaginatedArtifactList(List<RMOT> resources, Integer offset, Integer limit, Integer count,
                                 String url) {
        this(resources, offset, limit, count, url, "");
    }
    public PaginatedArtifactList(List<RMOT> resources, Integer offset, Integer limit, Integer count,
                                 String url, String suffix) {
        this.setResources(resources);
        this.setOffset(offset);
        this.setLimit(limit);
        this.setCount(count);

        AbstractPaginatedList.Link linkSystemNext = null;
        AbstractPaginatedList.Link linkSystemPrev = null;
        try {
            if(this.getCount() - this.getOffset() - this.getLimit() > 0)
                linkSystemNext = new AbstractPaginatedList.Link(new URI("..." + url + "?limit="  + this.getLimit() +
                        "&offset=" + (this.getOffset() + this.getLimit()) + suffix), this.getOffset() + this.getLimit());
            if(this.getOffset() < this.getCount() &&
                    this.getOffset() - this.getLimit() >= 0)
                linkSystemPrev = new AbstractPaginatedList.Link(new URI("..." + url + "?limit="  + this.getLimit() +
                        "&offset=" + (this.getOffset() - this.getLimit()) + suffix), this.getOffset() - this.getLimit());
        } catch (URISyntaxException e) {
            log.error(e.getMessage());
        }
        this.setNext(linkSystemNext);
        this.setPrev(linkSystemPrev);
    }

}

