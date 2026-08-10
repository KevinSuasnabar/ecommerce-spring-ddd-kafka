package com.ecommerce.catalog.application.service;

import com.ecommerce.catalog.application.dto.CategoryQueryResult;
import com.ecommerce.catalog.application.dto.ChangeProductPriceCommand;
import com.ecommerce.catalog.application.dto.CreateCategoryCommand;
import com.ecommerce.catalog.application.dto.CreateProductCommand;
import com.ecommerce.catalog.application.dto.ProductPageResult;
import com.ecommerce.catalog.application.dto.ProductQueryResult;
import com.ecommerce.catalog.application.dto.SearchProductsQuery;
import com.ecommerce.catalog.application.dto.UpdateProductCommand;
import com.ecommerce.catalog.application.port.out.EventPublisher;
import com.ecommerce.catalog.domain.event.ProductActivatedEvent;
import com.ecommerce.catalog.domain.event.ProductCreatedEvent;
import com.ecommerce.catalog.domain.event.ProductPriceChangedEvent;
import com.ecommerce.catalog.domain.exception.CategoryNotFoundException;
import com.ecommerce.catalog.domain.exception.DuplicateCategoryException;
import com.ecommerce.catalog.domain.exception.ProductNotFoundException;
import com.ecommerce.catalog.domain.model.*;
import com.ecommerce.catalog.domain.repository.CategoryRepository;
import com.ecommerce.catalog.domain.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CatalogApplicationServiceTest {

    private static final Currency USD = Currency.getInstance("USD");
    private static final CompanyId COMPANY_ID = new CompanyId(UUID.fromString("90000000-0000-0000-0000-000000000001"));
    private static final CompanyId OTHER_COMPANY_ID = new CompanyId(UUID.fromString("90000000-0000-0000-0000-000000000002"));

    @Mock
    private ProductRepository productRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private EventPublisher eventPublisher;

    private CatalogApplicationService service;

    @BeforeEach
    void setUp() {
        service = new CatalogApplicationService(productRepository, categoryRepository, eventPublisher);
    }

    @Test
    void createProductPersistsDraftAndPublishesCreatedEvent() {
        CreateProductCommand command = new CreateProductCommand(COMPANY_ID, "Notebook", "16GB", new BigDecimal("1500.00"), USD);

        ProductId productId = service.createProduct(command);

        ArgumentCaptor<Product> saved = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(saved.capture());
        assertThat(saved.getValue().id()).isEqualTo(productId);
        assertThat(saved.getValue().status()).isEqualTo(ProductStatus.DRAFT);
        verify(eventPublisher).publish(any(ProductCreatedEvent.class));
    }

    @Test
    void activateProductPublishesActivatedEvent() {
        Product product = Product.create(ProductId.newId(), "Notebook", null, new Money(new BigDecimal("1500.00"), USD), COMPANY_ID);
        product.pullDomainEvents();
        when(productRepository.findById(COMPANY_ID, product.id())).thenReturn(Optional.of(product));

        service.activateProduct(COMPANY_ID, product.id());

        assertThat(product.status()).isEqualTo(ProductStatus.ACTIVE);
        verify(eventPublisher).publish(any(ProductActivatedEvent.class));
        verify(productRepository).save(product);
    }

    @Test
    void changeProductPricePublishesPriceChangedEvent() {
        Product product = Product.create(ProductId.newId(), "Notebook", null, new Money(new BigDecimal("1500.00"), USD), COMPANY_ID);
        product.pullDomainEvents();
        when(productRepository.findById(COMPANY_ID, product.id())).thenReturn(Optional.of(product));

        service.changeProductPrice(COMPANY_ID, product.id(), new ChangeProductPriceCommand(new BigDecimal("1400.00"), USD));

        assertThat(product.price().amount()).isEqualByComparingTo("1400.00");
        verify(eventPublisher).publish(any(ProductPriceChangedEvent.class));
    }

    @Test
    void getUnknownProductThrows() {
        ProductId unknown = ProductId.newId();
        when(productRepository.findById(COMPANY_ID, unknown)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getProduct(COMPANY_ID, unknown))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void productOfAnotherCompanyIsNotVisible() {
        Product foreign = Product.create(ProductId.newId(), "Foreign Secret", null, new Money(new BigDecimal("1500.00"), USD), OTHER_COMPANY_ID);
        foreign.pullDomainEvents();
        when(productRepository.findById(COMPANY_ID, foreign.id())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getProduct(COMPANY_ID, foreign.id()))
                .isInstanceOf(ProductNotFoundException.class);
        assertThatThrownBy(() -> service.updateProduct(COMPANY_ID, foreign.id(), new UpdateProductCommand("hacked", null)))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void searchOnlySeesOwnCompanyProducts() {
        Product mine = product("Notebook", null, new Money(new BigDecimal("1500.00"), USD), ProductStatus.ACTIVE);
        Product foreign = Product.create(ProductId.newId(), "Foreign Secret", null, new Money(new BigDecimal("1.00"), USD), OTHER_COMPANY_ID);
        when(productRepository.findAllByCompanyId(COMPANY_ID)).thenReturn(List.of(mine));

        ProductPageResult result = service.search(COMPANY_ID, new SearchProductsQuery(null, null, null, 0, 10));

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).name()).isEqualTo("Notebook");
    }

    @Test
    void searchFiltersAndPaginates() {
        Product activeNotebook = product("Notebook", null, new Money(new BigDecimal("1500.00"), USD), ProductStatus.ACTIVE);
        Product activeMouse = product("Mouse", null, new Money(new BigDecimal("50.00"), USD), ProductStatus.ACTIVE);
        Product draftNotebook = product("Notebook Pro", null, new Money(new BigDecimal("2000.00"), USD), ProductStatus.DRAFT);
        when(productRepository.findAllByCompanyId(COMPANY_ID)).thenReturn(List.of(activeNotebook, activeMouse, draftNotebook));

        ProductPageResult result = service.search(COMPANY_ID, new SearchProductsQuery("notebook", null, null, 0, 10));

        assertThat(result.items()).hasSize(2);
        assertThat(result.totalElements()).isEqualTo(2);
        assertThat(result.totalPages()).isEqualTo(1);
    }

    @Test
    void searchFiltersByStatusAndPaginates() {
        Product activeNotebook = product("Notebook", null, new Money(new BigDecimal("1500.00"), USD), ProductStatus.ACTIVE);
        Product activeMouse = product("Mouse", null, new Money(new BigDecimal("50.00"), USD), ProductStatus.ACTIVE);
        Product retiredKeyboard = product("Keyboard", null, new Money(new BigDecimal("30.00"), USD), ProductStatus.RETIRED);
        when(productRepository.findAllByCompanyId(COMPANY_ID)).thenReturn(List.of(activeNotebook, activeMouse, retiredKeyboard));

        ProductPageResult result = service.search(COMPANY_ID, new SearchProductsQuery(null, null, ProductStatus.ACTIVE, 0, 1));

        assertThat(result.items()).hasSize(1);
        assertThat(result.totalElements()).isEqualTo(2);
        assertThat(result.totalPages()).isEqualTo(2);
    }

    @Test
    void createCategoryRejectsDuplicateName() {
        when(categoryRepository.existsByName(COMPANY_ID, "Computers")).thenReturn(true);

        assertThatThrownBy(() -> service.createCategory(new CreateCategoryCommand(COMPANY_ID, "Computers", null)))
                .isInstanceOf(DuplicateCategoryException.class);

        verify(categoryRepository, never()).save(any());
    }

    @Test
    void createCategoryHappyPath() {
        when(categoryRepository.existsByName(COMPANY_ID, "Computers")).thenReturn(false);

        CategoryId categoryId = service.createCategory(new CreateCategoryCommand(COMPANY_ID, "Computers", null));

        assertThat(categoryId).isNotNull();
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void getCategoriesReturnsOnlyOwnCompany() {
        Category mine = Category.create(CategoryId.newId(), "Computers", null, COMPANY_ID);
        Category foreign = Category.create(CategoryId.newId(), "Foreign", null, OTHER_COMPANY_ID);
        when(categoryRepository.findAllByCompanyId(COMPANY_ID)).thenReturn(List.of(mine));

        List<CategoryQueryResult> categories = service.getCategories(COMPANY_ID);

        assertThat(categories).hasSize(1);
        assertThat(categories.get(0).name()).isEqualTo("Computers");
    }

    @Test
    void assignCategoryToUnknownCategoryThrows() {
        Product product = Product.create(ProductId.newId(), "Notebook", null, new Money(new BigDecimal("1500.00"), USD), COMPANY_ID);
        CategoryId categoryId = CategoryId.newId();
        when(productRepository.findById(COMPANY_ID, product.id())).thenReturn(Optional.of(product));
        when(categoryRepository.findById(COMPANY_ID, categoryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.assignCategory(COMPANY_ID, product.id(), categoryId))
                .isInstanceOf(CategoryNotFoundException.class);
    }

    @Test
    void assignCategoryAssignsAndPersists() {
        Product product = Product.create(ProductId.newId(), "Notebook", null, new Money(new BigDecimal("1500.00"), USD), COMPANY_ID);
        Category category = Category.create(CategoryId.newId(), "Computers", null, COMPANY_ID);
        when(productRepository.findById(COMPANY_ID, product.id())).thenReturn(Optional.of(product));
        when(categoryRepository.findById(COMPANY_ID, category.id())).thenReturn(Optional.of(category));

        service.assignCategory(COMPANY_ID, product.id(), category.id());

        assertThat(product.categories()).containsExactly(category.id());
        verify(productRepository).save(product);
    }

    @Test
    void removeCategoryAssignsAndPersists(){
        Product product = Product.create(ProductId.newId(), "Notebook", null, new Money(new BigDecimal("1500.00"), USD), COMPANY_ID);
        Category category = Category.create(CategoryId.newId(), "Computers", null, COMPANY_ID);
        when(productRepository.findById(COMPANY_ID, product.id())).thenReturn(Optional.of(product));

        CategoryId categoryId = category.id();
        product.assignCategory(categoryId);
        service.removeCategory(COMPANY_ID, product.id(), categoryId);

        assertThat(product.categories()).isEmpty();
        verify(productRepository).save(product);
    }

    private Product product(String name, String description, Money price, ProductStatus status) {
        Product product = Product.create(ProductId.newId(), name, description, price, COMPANY_ID);
        product.pullDomainEvents();
        if (status == ProductStatus.ACTIVE) {
            product.activate();
        } else if (status == ProductStatus.RETIRED) {
            product.retire();
        }
        product.pullDomainEvents();
        return product;
    }
}
