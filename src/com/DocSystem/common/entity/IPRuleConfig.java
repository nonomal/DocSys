package com.DocSystem.common.entity;

import java.util.ArrayList;
import java.util.List;

import com.DocSystem.common.Log;

public class IPRuleConfig 
{
	public int type = 0;	//0: IPV4 1: IPV6
	public List<int[]> rangeList;
	
	public IPRuleConfig()
	{
		type = 0;
		rangeList = new ArrayList<int[]>();
	}

	public boolean isMatched(int[] ip_values) 
	{
		Log.debug("isMatched() rangList.size:" + rangeList.size());
		Log.debug("isMatched() ip_values.size:" + ip_values.length);
		if(rangeList.size() > ip_values.length)
		{
			return false;
		}
		
		for(int i = 0; i < rangeList.size(); i++)
		{
			int ipVal = ip_values[i];
			int[] range = rangeList.get(i);
			
			Log.debug("ipVal【" + ipVal + "】 range【" + range[0] + ", " + range[1] + "】");
			if(ipVal < range[0] || ipVal > range[1])
			{
				return false;
			}
		}
		
		return true;
	}
	
	public static IPRuleConfig parseIPRuleConfig(String iPRuleConfigStr) 
	{
		//192.168.3.1		//指定IP地址
		//192.168.3.1-10	//范围
		//192.168.3.*		//通配符
		//192.168.3-10.*	//范围+通配符
		Log.debug("parseIPRuleConfig() iPRuleConfigStr【" + iPRuleConfigStr + "】");
		String [] tempStr = iPRuleConfigStr.split("\\.");
		IPRuleConfig ipRuleConfig = new IPRuleConfig();
		//TODO: 我假设已经检查过规则
		for(int i=0; i<tempStr.length; i++)
		{
			int[] range = getIPRuleRange(tempStr[i]);
			if(range == null)
			{
				Log.debug("parseIPRuleConfig() 无效的IPRuleConfig【" + iPRuleConfigStr + "】");
				return null;
			}
			Log.debug("parseIPRuleConfig() rangeStr【" + tempStr[i] + "】 range【" +range[0]+ "," + range[1] + "】");
			ipRuleConfig.rangeList.add(range);
		}
		return ipRuleConfig;
	}

	private static int[] getIPRuleRange(String IPRuleRangeStr) 
	{		
		int[] range = new int[2];			
		
		IPRuleRangeStr = IPRuleRangeStr.trim();
		//任意值
		if(IPRuleRangeStr.equals("*"))
		{
			range[0] = 0;
			range[1] = 255;
			return range;
		}
		
		if(IPRuleRangeStr.indexOf("-") < 0)
		{
			range[0] = Integer.parseInt(IPRuleRangeStr);
			range[1] = range[0];
			return range;
		}
		
		String[] rangValueStr = IPRuleRangeStr.split("-");
		if(range.length < 2)
		{
			//无效配置
			Log.debug("getIPRuleRange() 无效的IPRuleRangeStr【" + IPRuleRangeStr + "】");
			return null;
		}
		
		range[0] = Integer.parseInt(rangValueStr[0]);
		range[1] = Integer.parseInt(rangValueStr[1]);		
		return range;
	}
}
