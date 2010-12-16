package br.com.caelum.vraptor.authz;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import br.com.caelum.vraptor.Result;
import br.com.caelum.vraptor.authz.annotation.AuthzBypass;
import br.com.caelum.vraptor.core.InterceptorStack;
import br.com.caelum.vraptor.http.route.Router;
import br.com.caelum.vraptor.resource.DefaultResourceClass;
import br.com.caelum.vraptor.resource.HttpMethod;
import br.com.caelum.vraptor.resource.ResourceMethod;

public class AuthzTest {

	@Mock
	private ResourceMethod resourceMethod;

	@Mock
	private ResourceMethod bypassedResourceMethod;

	@Mock
	private InterceptorStack stack;

	@Mock
	private Result result;

	@Mock
	private Authorizator authorizator;

	@Mock
	private Authorizable authorizable;

	@Mock
	private AuthzInfo authInfo;

	@Mock
	private Role admin;

	@Mock
	private Role user;

	@Mock
	private Router router;

	private Authz interceptor;
	private Set<Role> allRoles;
	private Set<Role> noRoles;

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);

		Mockito.when(router.allowedMethodsFor(Mockito.any(String.class))).thenReturn(EnumSet.of(HttpMethod.GET));
		interceptor = new Authz(authorizator, authInfo, result, router);
		Mockito.when(authorizator.isAllowed(admin, "/", EnumSet.of(HttpMethod.GET))).thenReturn(true);
		Mockito.when(authorizator.isAllowed(user, "/", EnumSet.of(HttpMethod.GET))).thenReturn(false);
		allRoles = new HashSet<Role>(Arrays.asList(admin, user));
		noRoles = new HashSet<Role>();
	}

	@SuppressWarnings("unchecked")
	@Test
	public void shouldNotAllowAccessWithoutRoles() throws SecurityException, NoSuchMethodException {
		Mockito.when(resourceMethod.getResource()).thenReturn(new DefaultResourceClass(FakeResource.class));
		Mockito.when(resourceMethod.getMethod()).thenReturn(FakeResource.class.getMethod("doIt"));
		Mockito.when(router.urlFor(Mockito.any(Class.class), Mockito.any(Method.class))).thenReturn("/");
		Mockito.when(authorizable.roles()).thenReturn(noRoles);
		Mockito.when(authInfo.getAuthorizable()).thenReturn(authorizable);
		interceptor.intercept(stack, resourceMethod, null);
		Mockito.verifyZeroInteractions(stack);
		Mockito.verify(authInfo).handleAuthError(result);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void shoudAllowAccessWithAdminRole() throws SecurityException, NoSuchMethodException {
		Mockito.when(resourceMethod.getResource()).thenReturn(new DefaultResourceClass(FakeResource.class));
		Mockito.when(resourceMethod.getMethod()).thenReturn(FakeResource.class.getMethod("doIt"));
		Mockito.when(router.urlFor(Mockito.any(Class.class), Mockito.any(Method.class))).thenReturn("/");
		Mockito.when(authorizable.roles()).thenReturn(allRoles);
		Mockito.when(authInfo.getAuthorizable()).thenReturn(authorizable);
		interceptor.intercept(stack, resourceMethod, null);
		Mockito.verify(stack).next(resourceMethod, null);
		Mockito.verify(authInfo, Mockito.never()).handleAuthError(result);
	}

	@Test
	public void shouldBypassAuthzIfAnnotatedWithBypass() throws SecurityException, NoSuchMethodException {
		Mockito.when(bypassedResourceMethod.getMethod()).thenReturn(FakeResource.class.getMethod("doIt"));
		Mockito.when(bypassedResourceMethod.getResource()).thenReturn(new DefaultResourceClass(FakeResource.class));
		Assert.assertFalse(interceptor.accepts(bypassedResourceMethod));
		Mockito.when(resourceMethod.getResource()).thenReturn(new DefaultResourceClass(FakeResource.class));
		Mockito.when(resourceMethod.getMethod()).thenReturn(FakeResource.class.getMethod("dontDoIt"));
		Assert.assertTrue(interceptor.accepts(resourceMethod));
	}

	static class FakeResource {
		@AuthzBypass
		public void doIt() {

		}

		public void dontDoIt() {

		}
	}

	@Test
	public void shouldBypassAuthzIfTypeIsAnnotatedWithBypass() throws SecurityException, NoSuchMethodException {
		Mockito.when(bypassedResourceMethod.getMethod()).thenReturn(CreativeCommonsResource.class.getMethod("modifyMe"));
		Mockito.when(bypassedResourceMethod.getResource()).thenReturn(new DefaultResourceClass(CreativeCommonsResource.class));
		Assert.assertFalse(interceptor.accepts(bypassedResourceMethod));
	}

	@AuthzBypass
	static class CreativeCommonsResource {
		public void modifyMe() {

		}
	}

	@Test
	public void shouldBypassAuthzIfAnnotationIsInTypeHierarchy() throws SecurityException, NoSuchMethodException {
		Mockito.when(bypassedResourceMethod.getMethod()).thenReturn(MyPhoto.class.getMethod("newWork"));
		Mockito.when(bypassedResourceMethod.getResource()).thenReturn(new DefaultResourceClass(MyPhoto.class));
		Assert.assertFalse(interceptor.accepts(bypassedResourceMethod));
	}

	static class MyPhoto extends CreativeCommonsResource {
		public void newWork() {
		}
	}

}
