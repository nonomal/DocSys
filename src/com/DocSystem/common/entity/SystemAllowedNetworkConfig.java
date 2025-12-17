package com.DocSystem.common.entity;

import java.util.ArrayList;
import java.util.List;

import com.DocSystem.common.Log;

public class SystemAllowedNetworkConfig 
{
	public boolean enabled = true;
	public List<IPRuleConfig> allowedNetworkList;
	
	public SystemAllowedNetworkConfig()
	{
		enabled = true;
		allowedNetworkList = new ArrayList<IPRuleConfig>();
	}

	public static boolean isAllowedNetwork(String ip, List<IPRuleConfig> allowedNetworkList) 
	{
		//TODO: 将IP地址解析成int[]数组
		int[] ip_values = getIPValues(ip);		

		for(IPRuleConfig ipRule : allowedNetworkList)
		{
			if(ipRule.isMatched(ip_values))
			{				
				return true;
			}
		}
		return false;
	}

	private static int[] getIPValues(String ip) 
	{
		String[] ipValArr = ip.split("\\.");
		int[] ip_values = new int[ipValArr.length];
		for(int i=0; i < ipValArr.length; i++)
		{
			ip_values[i] = Integer.parseInt(ipValArr[i]);
			Log.debug("getIPValues() ip_values[" + i + "]【" + ip_values[i] +"】");
		}
		return ip_values;
	}
}