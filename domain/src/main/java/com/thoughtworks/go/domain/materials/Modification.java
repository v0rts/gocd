/*
 * Copyright 2023 Thoughtworks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.thoughtworks.go.domain.materials;

import com.google.gson.reflect.TypeToken;
import com.thoughtworks.go.domain.MaterialInstance;
import com.thoughtworks.go.domain.ModificationVisitor;
import com.thoughtworks.go.domain.PersistentObject;
import com.thoughtworks.go.domain.materials.mercurial.StringRevision;
import com.thoughtworks.go.util.GoConstants;
import com.thoughtworks.go.util.json.JsonHelper;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.TestOnly;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;

/**
 * data structure for holding data about a single modification
 * to a source control tool.
 * <p/>
 * <modification type="" date="" user="" email="">
 * <comment></comment>
 * <file >
 * </modification>
 */
public class Modification extends PersistentObject implements Comparable<Modification>, Serializable {

    private static final long serialVersionUID = 6102576575583133520L;

    public static final Modification NEVER = new Modification(GoConstants.NEVER);
    public static final String ANONYMOUS = "anonymous";

    private String userName;
    private String comment;
    private String emailAddress;
    private String revision;
    private String additionalData;
    private Map<String, String> additionalDataMap;

    private Date modifiedTime;
    private Set<ModifiedFile> files = new LinkedHashSet<>();
    private MaterialInstance materialInstance;
    private String pipelineLabel;
    private Long pipelineId;

    public Modification() {
    }

    private Modification(Date datetime) {
        this.modifiedTime = datetime;
    }

    public Modification(Date date, String revision, String pipelineLabel, Long pipelineId) {
        this(null, null, null, date, revision);
        this.pipelineLabel = pipelineLabel;
        this.pipelineId = pipelineId;
    }

    public Modification(String user, String comment, String email, Date dateTime, String revision) {
        this.userName = user;
        this.comment = comment;
        this.emailAddress = email;
        this.modifiedTime = dateTime;
        this.revision = revision;
    }

    public Modification(Modification modification) {
        this(modification, true);
    }

    public Modification(String user, String comment, String email, Date dateTime, String revision, String additionalData) {
        this(user, comment, email, dateTime, revision);
        setAdditionalData(additionalData);
    }

    public Modification(Modification modification, boolean shouldCopyModifiedFiles) {
        this(modification.userName, modification.comment, modification.emailAddress, modification.modifiedTime, modification.getRevision());
        this.id = modification.id;
        if (shouldCopyModifiedFiles) {
            this.files = modification.files;
        }
        this.pipelineLabel = modification.pipelineLabel;
        this.pipelineId = modification.pipelineId;
        this.materialInstance = modification.materialInstance;
        this.additionalData = modification.additionalData;
        this.additionalDataMap = modification.additionalDataMap;
    }

    public final ModifiedFile createModifiedFile(String filename, String folder, ModifiedAction modifiedAction) {
        ModifiedFile file = new ModifiedFile(filename, folder, modifiedAction);
        files.add(file);
        return file;
    }

    public Map<String, String> getAdditionalDataMap() {
        return additionalDataMap == null ? new HashMap<>() : additionalDataMap;
    }


    public void setAdditionalData(String additionalData) {
        this.additionalData = additionalData;
        this.additionalDataMap = JsonHelper.safeFromJson(this.additionalData, new TypeToken<HashMap<String, String>>() {}.getType());
    }

    public void setUserName(String name) {
        this.userName = name;
    }

    public void setEmailAddress(String email) {
        this.emailAddress = email;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public void setRevision(String revision) {
        this.revision = revision;
    }

    public void setModifiedTime(Date modifiedTime) {
        this.modifiedTime = modifiedTime;
    }

    public void setModifiedFiles(List<ModifiedFile> files) {
        this.files = files == null ? new LinkedHashSet<>() : new LinkedHashSet<>(files);
    }

    /**
     * Returns the list of modified files for this modification set.
     *
     * @return list of {@link ModifiedFile} objects. If there are no files, this returns an empty list
     * (<code>null</code> is never returned).
     */
    public List<ModifiedFile> getModifiedFiles() {
        return List.copyOf(files);
    }

    @Override
    public int compareTo(Modification o) {
        return modifiedTime.compareTo(o.modifiedTime);
    }

    public Date getModifiedTime() {
        return modifiedTime;
    }

    public String getUserName() {
        return userName;
    }

    public String getUserDisplayName() {
        return StringUtils.isBlank(userName) ? ANONYMOUS : userName;
    }

    public String getRevision() {
        return revision;
    }

    public Long getPipelineId() {
        return pipelineId;
    }

    public String getComment() {
        return comment;
    }

    @Override
    public String toString() {
        SimpleDateFormat formatter =
            new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        StringBuilder sb = new StringBuilder();
        if (materialInstance != null) {
            sb.append("Material: ").append(materialInstance).append('\n');
        }
        String timeString = modifiedTime == null ? "" : formatter.format(modifiedTime);
        sb.append("Last Modified: ").append(timeString).append('\n');
        sb.append("Revision: ").append(revision).append('\n');
        sb.append("UserName: ").append(userName).append('\n');
        sb.append("EmailAddress: ").append(emailAddress).append('\n');
        sb.append("Comment: ").append(comment).append('\n');
        sb.append("PipelineLabel: ").append(pipelineLabel).append('\n');
        return sb.toString();
    }

    public static Revision latestRevision(List<Modification> modifications) {
        if (modifications.isEmpty()) {
            throw new RuntimeException("Cannot find latest revision.");
        } else {
            return new StringRevision(modifications.get(0).getRevision());
        }
    }

    public void accept(ModificationVisitor visitor) {
        visitor.visit(this);
        for (ModifiedFile file : files) {
            visitor.visit(file);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Modification)) {
            return false;
        }

        Modification that = (Modification) o;

        // Doesn't include additionalDataMap or materialInstance
        return Objects.equals(userName, that.userName) &&
            Objects.equals(comment, that.comment) &&
            Objects.equals(emailAddress, that.emailAddress) &&
            Objects.equals(revision, that.revision) &&
            Objects.equals(additionalData, that.additionalData) &&
            Objects.equals(modifiedTime, that.modifiedTime) &&
            Objects.equals(files, that.files) &&
            Objects.equals(pipelineLabel, that.pipelineLabel) &&
            Objects.equals(pipelineId, that.pipelineId);
    }

    @Override
    public int hashCode() {
        // Doesn't include additionalDataMap or materialInstance
        int result = userName != null ? userName.hashCode() : 0;
        result = 31 * result + (comment != null ? comment.hashCode() : 0);
        result = 31 * result + (emailAddress != null ? emailAddress.hashCode() : 0);
        result = 31 * result + (revision != null ? revision.hashCode() : 0);
        result = 31 * result + (additionalData != null ? additionalData.hashCode() : 0);
        result = 31 * result + (modifiedTime != null ? modifiedTime.hashCode() : 0);
        result = 31 * result + (files != null ? files.hashCode() : 0);
        result = 31 * result + (pipelineLabel != null ? pipelineLabel.hashCode() : 0);
        result = 31 * result + (pipelineId != null ? pipelineId.hashCode() : 0);
        return result;
    }

    public void setMaterialInstance(MaterialInstance materialInstance) {
        this.materialInstance = materialInstance;
    }

    public MaterialInstance getMaterialInstance() {
        return materialInstance;
    }

    public static ArrayList<Modification> modifications(Modification modification) {
        ArrayList<Modification> modifications = new ArrayList<>();
        modifications.add(modification);
        return modifications;
    }

    public boolean isSameRevision(Modification that) {
        return Objects.equals(revision, that.revision);
    }

    public String getPipelineLabel() {
        return pipelineLabel;
    }

    @TestOnly
    public void setPipelineLabel(String pipelineLabel) {
        this.pipelineLabel = pipelineLabel;
    }

    public String id(Matcher matcher) {
        return matcher.groupCount() > 0 ? contentsOfFirstGroupThatMatched(matcher) : matcher.group();
    }

    private String contentsOfFirstGroupThatMatched(Matcher matcher) {
        for (int i = 1; i <= matcher.groupCount(); i++) {
            String groupContent = matcher.group(i);
            if (groupContent != null) {
                return groupContent;
            }
        }
        return null;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public static Revision oldestRevision(Modifications modifications) {
        if (modifications.isEmpty()) {
            throw new RuntimeException("Cannot find oldest revision.");
        } else {
            return new StringRevision(modifications.get(modifications.size() - 1).getRevision());
        }
    }

    public String getAdditionalData() {
        return additionalData;
    }
}
