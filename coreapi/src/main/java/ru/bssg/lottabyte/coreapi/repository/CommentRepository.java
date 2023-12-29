package ru.bssg.lottabyte.coreapi.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.model.comment.Comment;
import ru.bssg.lottabyte.core.model.comment.FlatComment;
import ru.bssg.lottabyte.core.model.comment.UpdatableComment;
import ru.bssg.lottabyte.core.ui.model.SearchColumn;
import ru.bssg.lottabyte.core.ui.model.SearchColumnForJoin;
import ru.bssg.lottabyte.core.ui.model.SearchRequestWithJoin;
import ru.bssg.lottabyte.core.ui.model.SearchResponse;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;
import ru.bssg.lottabyte.core.util.ServiceUtils;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Repository
@Slf4j
@RequiredArgsConstructor
public class CommentRepository {
    private final JdbcTemplate jdbcTemplate;

    class CommentRowMapper implements RowMapper<Comment> {
        @Override
        public Comment mapRow(ResultSet rs, int rowNum) throws SQLException {
            Comment comment = new Comment();

            comment.setArtifactId(rs.getString("artifact_id"));
            comment.setArtifactType(getDataArtifactType(rs.getString("artifact_type")));
            comment.setCommentText(rs.getString("comment_text"));
            comment.setId(rs.getString("id"));
            comment.setParentCommentId(rs.getString("parent_comment_id"));
            comment.setModified(rs.getTimestamp("modified").toLocalDateTime());
            comment.setModifier(rs.getString("modifier"));
            comment.setModified(rs.getTimestamp("created").toLocalDateTime());
            comment.setModifier(rs.getString("creator"));

            return comment;
        }
    }

    public void deleteAllCommentsByArtifactId(String artifactId, UserDetails userDetails) {
        jdbcTemplate.update("DELETE FROM da_" + userDetails.getTenant() + ".comment WHERE artifact_id = ? ",
                UUID.fromString(artifactId));
    }

    public Comment getCommentById(String commentId, UserDetails userDetails) {
        return jdbcTemplate.queryForObject("SELECT * FROM da_" + userDetails.getTenant() + ".comment WHERE id=?",
                new CommentRepository.CommentRowMapper(), UUID.fromString(commentId));
    }

    public Boolean existsObjectByIdAndArtifactType(String artifactId, String artifactType, UserDetails userDetails) {
        return jdbcTemplate.queryForObject("SELECT EXISTS(SELECT id FROM da_" + userDetails.getTenant() + "." + artifactType + " WHERE id = ?) as exists",
                Boolean.class, UUID.fromString(artifactId));
    }

    public Boolean existCommentsByArtifactId(String artifactId, UserDetails userDetails) {
        return jdbcTemplate.queryForObject("SELECT EXISTS(SELECT id FROM da_" + userDetails.getTenant() + ".comment WHERE artifact_id = ?) as exists",
                Boolean.class, UUID.fromString(artifactId));
    }

    public Boolean existCommentsById(String commentId, UserDetails userDetails) {
        return jdbcTemplate.queryForObject("SELECT EXISTS(SELECT id FROM da_" + userDetails.getTenant() + ".comment WHERE id = ?) as exists",
                Boolean.class, UUID.fromString(commentId));
    }

    public List<Comment> getCommentsByArtifactId(String artifactId, UserDetails userDetails) {
        List<Comment> commentList =
                jdbcTemplate.query("SELECT * " +
                                "FROM da_" + userDetails.getTenant() + ".comment \n" +
                                "where artifact_id = ?",
                        new CommentRepository.CommentRowMapper(), UUID.fromString(artifactId));

        if(commentList.size() > 1){
            commentList.sort(Comparator.comparing(Comment::getModified));
        }
        return commentList;
    }

    public ArtifactType getDataArtifactType(String artifactType) {
        ArtifactType dataArtifactType = null;
        try {
            dataArtifactType = ArtifactType.valueOf(artifactType);
        } catch (IllegalArgumentException e) {
            return null;
        }
        return dataArtifactType;
    }

    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public Comment createComment(UpdatableComment comment, UserDetails userDetails) {
        UUID id = UUID.randomUUID();

        Comment c = new Comment();
        c.setId(id.toString());
        LocalDateTime now = LocalDateTime.now();
        c.setModified(now);
        c.setCommentText(comment.getCommentText());
        c.setModifier(userDetails.getUid());
        c.setParentCommentId(comment.getParentCommentId());
        c.setArtifactType(c.getArtifactType());
        c.setArtifactId(c.getArtifactId());
        c.setCreated(now);
        c.setCreator(userDetails.getUid());

        jdbcTemplate.update("INSERT INTO da_" + userDetails.getTenant() + ".comment " +
                        "(id, comment_text, parent_comment_id, artifact_id, artifact_type, modified, modifier, created, creator) " +
                        "VALUES (?,?,?,?,?,?,?,?,?)",
                id, comment.getCommentText(), (comment.getParentCommentId() == null || comment.getParentCommentId().isEmpty()) ? null : UUID.fromString(comment.getParentCommentId()), UUID.fromString(comment.getArtifactId()), comment.getArtifactType(),
                new Timestamp(new java.util.Date().getTime()), c.getModifier(), new Timestamp(new java.util.Date().getTime()), c.getCreator());

        return c;
    }

    public void patchComment(String commentId, UpdatableComment updatableComment, UserDetails userDetails) {
        List<String> sets = new ArrayList<>();
        List<Object> params = new ArrayList<>();

        String query = "UPDATE da_" + userDetails.getTenant() + ".\"comment\" SET modifier = ?, modified = ?";
        params.add(userDetails.getUid());
        params.add(new Timestamp(new java.util.Date().getTime()));
        if (updatableComment.getCommentText() != null) {
            sets.add("\"comment_text\" = ?");
            params.add(updatableComment.getCommentText());
        }
        if (!sets.isEmpty()) {
            query += ", " + String.join(",", sets);
            query += " WHERE id = ?";
            params.add(UUID.fromString(commentId));
            jdbcTemplate.update(query, params.toArray());
        }
    }

    public void deleteComment(String commentId, UserDetails userDetails) {
        jdbcTemplate.update("DELETE FROM da_" + userDetails.getTenant() + ".comment WHERE id=?", UUID.fromString(commentId));
    }

    public SearchResponse<FlatComment> searchComment(SearchRequestWithJoin searchRequest, SearchColumn[] searchableColumns, SearchColumnForJoin[] searchableColumnsForJoin, UserDetails userDetails) {
        String orderby = "name";
        if (!StringUtils.isEmpty(searchRequest.getSort()))
            orderby = searchRequest.getSort().replaceAll("[\\-\\+]", "") + ((searchRequest.getSort().contains("-")) ? " DESC" : " ASC");

        Map<String, List<Object>> wheresMap = ServiceUtils.buildWhereForSearchRequestWithJoin(searchRequest, searchableColumns);
        String where = "";
        List<Object> vals = new ArrayList<>();
        for (String key : wheresMap.keySet()) {
            where = key;
            vals = wheresMap.get(key);
        }
        String join = ServiceUtils.buildJoinForSearchRequestWithJoin(searchRequest, userDetails);
        where = ServiceUtils.updateWhereForSearchRequestWithJoin(searchRequest, where);
        List<Comment> items = new ArrayList<>();

        String queryForItems = "SELECT * FROM da_" + userDetails.getTenant() + ".comment tbl1 " + join
                + where + " ORDER BY " + orderby + " OFFSET " + searchRequest.getOffset() + " LIMIT "
                + searchRequest.getLimit();

        jdbcTemplate.query(
                queryForItems,
                new RowCallbackHandler() {
                    @Override
                    public void processRow(ResultSet rs) throws SQLException {
                        Comment comment = new CommentRepository.CommentRowMapper().mapRow(rs, 0);
                        items.add(comment);
                    }
                },
                vals.toArray()
        );

        int total = ServiceUtils.getTotalForSearchRequestWithJoin(jdbcTemplate, userDetails, "comment", join, where, vals);

        SearchResponse<FlatComment> res = new SearchResponse<>();
        res.setCount(total);
        res.setLimit(searchRequest.getLimit());
        res.setOffset(searchRequest.getOffset());

        List<FlatComment> flatItems = items.stream().map(FlatComment::new).collect(Collectors.toList());
        int num = searchRequest.getOffset() + 1;
        for (FlatComment fs : flatItems)
            fs.setNum(num++);

        res.setItems(flatItems);

        return res;
    }
}
