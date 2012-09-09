/**
 * Copyright (C) 2011 BonitaSoft S.A.
 * BonitaSoft, 31 rue Gustave Eiffel - 38000 Grenoble
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2.0 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.bonitasoft.engine.cmis;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.web.AbstractFilter;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.Authenticator;
import org.exoplatform.services.security.ConversationRegistry;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityRegistry;
import org.exoplatform.services.security.StateKey;
import org.exoplatform.services.security.web.HttpSessionStateKey;

/**
 * @author Baptiste Mesta
 * 
 */
public class BonitaCurrentIdentityFilter extends AbstractFilter {

  private boolean restoreIdentity;

  /**
   * Logger.
   */
  private static Log log = ExoLogger.getLogger("exo.core.component.security.core.SetCurrentIdentityFilter");

  /**
   * {@inheritDoc}
   */
  @Override
  protected void afterInit(final FilterConfig config) throws ServletException {
    super.afterInit(config);
    restoreIdentity = Boolean.parseBoolean(config.getInitParameter("restoreIdentity"));
  }

  /*
   * (non-Javadoc)
   * @see javax.servlet.Filter#destroy()
   */
  @Override
  public void destroy() {
    // TODO Auto-generated method stub

  }

  /*
   * (non-Javadoc)
   * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest, javax.servlet.ServletResponse,
   * javax.servlet.FilterChain)
   */
  @Override
  public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
      throws IOException, ServletException {

    final HttpServletRequest httpRequest = (HttpServletRequest) request;
    final ExoContainer container = getContainer();

    try {
      ExoContainerContext.setCurrentContainer(container);
      final ConversationState state = getCurrentState(container, httpRequest);
      ConversationState.setCurrent(state);
      chain.doFilter(request, response);
    } finally {
      try {
        ConversationState.setCurrent(null);
      } catch (final Exception e) {
        log.warn("An error occured while cleaning the ThreadLocal", e);
      }
      try {
        ExoContainerContext.setCurrentContainer(null);
      } catch (final Exception e) {
        log.warn("An error occured while cleaning the ThreadLocal", e);
      }
    }
  }

  /**
   * Gives the current state
   */
  private ConversationState getCurrentState(final ExoContainer container, final HttpServletRequest httpRequest) {
    final ConversationRegistry conversationRegistry = (ConversationRegistry) container
        .getComponentInstanceOfType(ConversationRegistry.class);

    final IdentityRegistry identityRegistry = (IdentityRegistry) container
        .getComponentInstanceOfType(IdentityRegistry.class);

    ConversationState state = null;
    String userId = httpRequest.getRemoteUser();

    // only if user authenticated, otherwise there is no reason to do anythings
    if (userId != null) {
      final HttpSession httpSession = httpRequest.getSession();
      final StateKey stateKey = new HttpSessionStateKey(httpSession);

      if (log.isDebugEnabled()) {
        log.debug("Looking for Conversation State " + httpSession.getId());
      }

      state = conversationRegistry.getState(stateKey);

      if (state == null) {
        if (log.isDebugEnabled()) {
          log.debug("Conversation State not found, try create new one.");
        }

        Identity identity = identityRegistry.getIdentity(userId);
        if (identity == null) {
          int index = 0;
          if ((index = userId.lastIndexOf('#')) > -1) {
            userId = userId.substring(index + 1);
            identity = identityRegistry.getIdentity(userId);
          }
        }
        if (identity != null) {
          state = new ConversationState(identity);
          // Keep subject as attribute in ConversationState.
          // TODO remove this, do not need it any more.
          state.setAttribute(ConversationState.SUBJECT, identity.getSubject());
        } else {

          if (restoreIdentity) {
            if (log.isDebugEnabled()) {
              log.debug("Not found identity for " + userId + " try to restore it. ");
            }

            final Authenticator authenticator = (Authenticator) container
                .getComponentInstanceOfType(Authenticator.class);
            try {
              identity = authenticator.createIdentity(userId);
              identityRegistry.register(identity);
            } catch (final Exception e) {
              log.error("Unable restore identity. " + e.getMessage(), e);
            }

            if (identity != null) {
              state = new ConversationState(identity);
            }
          } else {
            log.error("Not found identity in IdentityRegistry for user " + userId + ", check Login Module.");
          }
        }

        if (state != null) {
          conversationRegistry.register(stateKey, state);
          if (log.isDebugEnabled()) {
            log.debug("Register Conversation state " + httpSession.getId());
          }
        }
      }
    } else {
      state = new ConversationState(new Identity("__anonim"));
    }
    return state;
  }

}
