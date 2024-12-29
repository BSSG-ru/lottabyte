package ru.bssg.lottabyte.coreapi.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.model.userfav.UserFav;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;
import ru.bssg.lottabyte.coreapi.repository.ArtifactRepository;
import ru.bssg.lottabyte.coreapi.repository.UserFavRepository;

import java.util.List;

@Service
@Slf4j
public class UserFavService {
    private final UserFavRepository userFavRepository;
    private final ArtifactRepository artifactRepository;

    @Autowired
    @Lazy
    public UserFavService(UserFavRepository userFavRepository,
                          ArtifactRepository artifactRepository) {
        this.userFavRepository = userFavRepository;
        this.artifactRepository = artifactRepository;
    }

    public List<UserFav> getUserFavs(String artifactType, UserDetails userDetails) {
        List<UserFav> res = userFavRepository.getUserFavs(Integer.parseInt(userDetails.getUid()), artifactType, userDetails);

        for (UserFav uf : res) {
            uf.setArtifactName(artifactRepository.getArtifactName(ArtifactType.valueOf(uf.getArtifactType()), uf.getArtifactId(), userDetails));
        }

        return res;
    }

    public void addUserFav(String artifactId, String artifactType, UserDetails userDetails) {
        userFavRepository.addUserFav(artifactId, artifactType, Integer.parseInt(userDetails.getUid()), userDetails);
    }

    public void delUserFav(String artifactId, UserDetails userDetails) {
        userFavRepository.delUserFav(Integer.parseInt(userDetails.getUid()), artifactId, userDetails);
    }

    public Boolean isInFav(String artifactId, UserDetails userDetails) {
        return userFavRepository.isInFav(artifactId, userDetails);
    }
}
