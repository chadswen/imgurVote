package com.chadswen.imgurvote.model;

import java.util.ArrayList;
import java.util.List;
import com.google.gson.annotations.Expose;

public class Gallery {

    @Expose
    private List<GalleryImage> galleryImages = new ArrayList<GalleryImage>();
    @Expose
    private Boolean success;
    @Expose
    private Integer status;

    /**
     *
     * @return
     * The data
     */
    public List<GalleryImage> getGalleryImages() {
        return galleryImages;
    }

    /**
     *
     * @param data
     * The data
     */
    public void setGalleryImages(List<GalleryImage> data) {
        this.galleryImages = galleryImages;
    }

    /**
     *
     * @return
     * The success
     */
    public Boolean getSuccess() {
        return success;
    }

    /**
     *
     * @param success
     * The success
     */
    public void setSuccess(Boolean success) {
        this.success = success;
    }

    /**
     *
     * @return
     * The status
     */
    public Integer getStatus() {
        return status;
    }

    /**
     *
     * @param status
     * The status
     */
    public void setStatus(Integer status) {
        this.status = status;
    }

}