package ru.bssg.lottabyte.coreapi.controller;

import org.flowable.engine.repository.Deployment;

import java.util.Date;

public class DeploymentResponse {

    protected String id;
    protected String name;
    protected Date deploymentTime;
    protected String category;
    protected String parentDeploymentId;
    protected String url;
    protected String tenantId;

    public DeploymentResponse() {
    }

    public DeploymentResponse(Deployment deployment, String url) {
        setId(deployment.getId());
        setName(deployment.getName());
        setDeploymentTime(deployment.getDeploymentTime());
        setCategory(deployment.getCategory());
        setParentDeploymentId(deployment.getParentDeploymentId());
        setTenantId(deployment.getTenantId());
        setUrl(url);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getDeploymentTime() {
        return deploymentTime;
    }

    public void setDeploymentTime(Date deploymentTime) {
        this.deploymentTime = deploymentTime;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getParentDeploymentId() {
        return parentDeploymentId;
    }

    public void setParentDeploymentId(String parentDeploymentId) {
        this.parentDeploymentId = parentDeploymentId;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getTenantId() {
        return tenantId;
    }

}
