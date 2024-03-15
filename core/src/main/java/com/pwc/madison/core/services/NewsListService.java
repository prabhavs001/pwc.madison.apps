package com.pwc.madison.core.services;

import java.util.List;

import javax.jcr.RepositoryException;

import org.osgi.annotation.versioning.ConsumerType;

import com.pwc.madison.core.models.NewsListItem;
import com.pwc.madison.core.userreg.models.UserProfile;

/**
 *
 *
 */
@ConsumerType
public interface NewsListService {
		
	/**
	 * Method to get the filtered news list based on the given filters
	 * 
	 * @param dynamicList
	 * 
	 * @param fallbackList
	 * 
	 * @param user
	 * @param dateFormat 
	 * 
	 * @return 
	 *      {@link List<NewsListItem>} returns null, if there are no filtered list items.
	 *      
	 * @throws RepositoryException
	 */
	public List<NewsListItem> getFilteredList(final List<String> dynamicList, final List<String> fallbackList, 
			final UserProfile user, final String dateFormat) throws RepositoryException;
	
}
