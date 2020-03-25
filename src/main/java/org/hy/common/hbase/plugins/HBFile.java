package org.hy.common.hbase.plugins;

import java.util.Map;

import org.hy.common.Date;




/**
 * 大文件信息对象  
 *
 * @author      ZhengWei(HY)
 * @createDate  2015-03-15
 * @version     v1.0
 */
public class HBFile
{
    
    /** 文件ID(一般为行主键) */
    private String              id;
    
    /** 文件名称 */
    private String              fileName;
    
    /** 文件总大小(单位：字节) */
    private Long                fileSize;
    
    /** 文件类型 */
    private String              fileType;
    
    /** 文件被分割总大小 */
    private Integer             segmentSize;
    
    /** 文件创建时间 */
    private Date                createTime;
    
    /** 其它数据信息。可为空。Map.key，即为字段名称，Map.value即为字段值 */
    private Map<String ,Object> elseDatas;
    
    
    
    /**
     * 获取：文件ID(一般为行主键)
     */
    public String getId()
    {
        return id;
    }
    
    
    
    /**
     * 设置：文件ID(一般为行主键)
     * 
     * @param id 
     */
    public void setId(String id)
    {
        this.id = id;
    }



    /**
     * 获取：文件名称
     */
    public String getFileName()
    {
        return fileName;
    }

    
    
    /**
     * 设置：文件名称
     * 
     * @param fileName 
     */
    public void setFileName(String fileName)
    {
        this.fileName = fileName;
    }

    
    
    /**
     * 获取：文件总大小(单位：字节)
     */
    public Long getFileSize()
    {
        return fileSize;
    }

    
    
    /**
     * 设置：文件总大小(单位：字节)
     * 
     * @param fileSize 
     */
    public void setFileSize(Long fileSize)
    {
        this.fileSize = fileSize;
    }

    
    
    /**
     * 获取：文件类型
     */
    public String getFileType()
    {
        return fileType;
    }

    
    
    /**
     * 设置：文件类型
     * 
     * @param fileType 
     */
    public void setFileType(String fileType)
    {
        this.fileType = fileType;
    }



    /**
     * 获取：文件被分割总大小
     */
    public Integer getSegmentSize()
    {
        return segmentSize;
    }

    
    
    /**
     * 设置：文件被分割总大小
     * 
     * @param segmentSize 
     */
    public void setSegmentSize(Integer segmentSize)
    {
        this.segmentSize = segmentSize;
    }


    
    /**
     * 获取：文件创建时间
     */
    public Date getCreateTime()
    {
        return createTime;
    }


    
    /**
     * 设置：文件创建时间
     * 
     * @param createTime 
     */
    public void setCreateTime(Date createTime)
    {
        this.createTime = createTime;
    }


    
    /**
     * 获取：其它数据信息。可为空。Map.key，即为字段名称，Map.value即为字段值
     */
    public Map<String ,Object> getElseDatas()
    {
        return elseDatas;
    }



    /**
     * 设置：其它数据信息。可为空。Map.key，即为字段名称，Map.value即为字段值
     * 
     * @param elseDatas 
     */
    public void setElseDatas(Map<String ,Object> elseDatas)
    {
        this.elseDatas = elseDatas;
    }
    
}
