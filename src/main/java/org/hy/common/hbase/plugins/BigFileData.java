package org.hy.common.hbase.plugins;





/**
 * 大文件数据的表信息(文件数据会被分割为多个小段数据保存)
 *
 * @author      ZhengWei(HY)
 * @createDate  2015-03-14
 * @version     v1.0
 */
public interface BigFileData
{
    
    /**
     * 获取文件数据表的名称
     * 
     * @author      ZhengWei(HY)
     * @createDate  2015-03-14
     * @version     v1.0
     *
     * @return
     */
    public String getTableName();
    
    
    
    /**
     * 获取文件数据表的列簇名称
     * 
     * @author      ZhengWei(HY)
     * @createDate  2015-03-14
     * @version     v1.0
     *
     * @return
     */
    public String getFamilyName();
    
    
    
    /**
     * 获取文件数据表的数据块的大小(单位：KB)
     * 
     * HBase存在数据块限制，需要根据应用进行调整。
     * 默认情况下，数据块限制为64KB。
     * 由于图片内容作为单元格(Cell)的值保存，其大小受制于数据块的大小。
     * 在应用中需根据最大图片大小对HBase数据块大小进行修改。
     * 用HColumnDescriptor.setBlocksize指定数据块大小，可分列簇指定。
     * 
     * @author      ZhengWei(HY)
     * @createDate  2015-03-14
     * @version     v1.0
     *
     * @return
     */
    public int getFamilyBlockSize();
    
}
