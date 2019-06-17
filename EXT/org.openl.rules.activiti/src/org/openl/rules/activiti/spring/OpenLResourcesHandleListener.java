package org.openl.rules.activiti.spring;

import org.activiti.engine.delegate.event.ActivitiEntityEvent;
import org.activiti.engine.delegate.event.ActivitiEvent;
import org.activiti.engine.delegate.event.ActivitiEventListener;
import org.activiti.engine.delegate.event.ActivitiEventType;
import org.activiti.engine.impl.persistence.entity.DeploymentEntity;
import org.activiti.engine.impl.persistence.entity.ResourceEntity;

public class OpenLResourcesHandleListener implements ActivitiEventListener {
    @Override
    public void onEvent(ActivitiEvent event) {
        if (event instanceof ActivitiEntityEvent) {
            Object entity = ((ActivitiEntityEvent) event).getEntity();
            if (entity instanceof ResourceEntity && (event.getType().equals(ActivitiEventType.ENTITY_UPDATED) || event
                .getType()
                .equals(ActivitiEventType.ENTITY_DELETED))) {
                ResourceEntity resourceEntity = (ResourceEntity) entity;
                OpenLRulesHelper.getInstance().clear(resourceEntity.getDeploymentId(), resourceEntity.getName());
            }

            if (entity instanceof DeploymentEntity && (event.getType().equals(ActivitiEventType.ENTITY_UPDATED) || event
                .getType()
                .equals(ActivitiEventType.ENTITY_DELETED))) {
                DeploymentEntity resourceEntity = (DeploymentEntity) entity;
                OpenLRulesHelper.getInstance().clear(resourceEntity.getId());
            }
        }
    }

    @Override
    public boolean isFailOnException() {
        return false;
    }
}
