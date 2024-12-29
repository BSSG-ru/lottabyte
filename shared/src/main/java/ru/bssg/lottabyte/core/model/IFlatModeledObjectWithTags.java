package ru.bssg.lottabyte.core.model;

import java.util.List;

public interface IFlatModeledObjectWithTags {
    public List<String> getTags();
    public void setTags(List<String> tags);
}
