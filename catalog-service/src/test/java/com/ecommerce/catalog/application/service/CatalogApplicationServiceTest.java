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
import com.ecommerce.catalog.domain.model.Category;
import com.ecommerce.catalog.domain.model.CategoryId;
import com.ecommerce.catalog.domain.model.Money;
import com.ecommerce.catalog.domain.model.Product;
import com.ecommerce.catalog.domain.model.ProductId;
import com.ecommerce.catalog.domain.model.ProductStatus;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogApplicationServiceTest {

    private static final Currency USD = Currency.getInstance("USD");

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
        CreateProductCommand command = new CreateProductCommand("Notebook", "16GB", new BigDecimal("1500.00"), USD);

        ProductId productId = service.createProduct(command);

        ArgumentCaptor<Product> saved = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(saved.capture());
        assertThat(saved.getValue().id()).isEqualTo(productId);
        assertThat(saved.getValue().status()).isEqualTo(ProductStatus.DRAFT);
        verify(eventPublisher).publish(any(ProductCreatedEvent.class));
    }

    @Test
    void activateProductPublishesActivatedEvent() {
        Product product = Product.create(ProductId.newId(), "Notebook", null, new Money(new BigDecimal("1500.00"), USD));
        product.pullDomainEvents();
        when(productRepository.findById(product.id())).thenReturn(Optional.of(product));

        service.activateProduct(product.id());

        assertThat(product.status()).isEqualTo(ProductStatus.ACTIVE);
        verify(eventPublisher).publish(any(ProductActivatedEvent.class));
        verify(productRepository).save(product);
    }

    @Test
    void changeProductPricePublishesPriceChangedEvent() {
        Product product = Product.create(ProductId.newId(), "Notebook", null, new Money(new BigDecimal("1500.00"), USD));
        product.pullDomainEvents();
        when(productRepository.findById(product.id())).thenReturn(Optional.of(product));

        service.changeProductPrice(product.id(), new ChangeProductPriceCommand(new BigDecimal("1400.00"), USD));

        assertThat(product.price().amount()).isEqualByComparingTo("1400.00");
        verify(eventPublisher).publish(any(ProductPriceChangedEvent.class));
    }

    @Test
    void getUnknownProductThrows() {
        ProductId unknown = ProductId.newId();
        when(productRepository.findById(unknown)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getProduct(unknown))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void searchFiltersAndPaginates() {
        Product activeNotebook = product("Notebook", null, new Money(new BigDecimal("1500.00"), USD), ProductStatus.ACTIVE);
        Product activeMouse = product("Mouse", null, new Money(new BigDecimal("50.00"), USD), ProductStatus.ACTIVE);
        Product draftNotebook = product("Notebook Pro", null, new Money(new BigDecimal("2000.00"), USD), ProductStatus.DRAFT);
        when(productRepository.findAll()).thenReturn(List.of(activeNotebook, activeMouse, draftNotebook));

        ProductPageResult result = service.search(new SearchProductsQuery("notebook", null, null, 0, 10));

        assertThat(result.items()).hasSize(2);
        assertThat(result.totalElements()).isEqualTo(2);
        assertThat(result.totalPages()).isEqualTo(1);
    }

    @Test
    void searchFiltersByStatusAndPaginates() {
        Product activeNotebook = product("Notebook", null, new Money(new BigDecimal("1500.00"), USD), ProductStatus.ACTIVE);
        Product activeMouse = product("Mouse", null, new Money(new BigDecimal("50.00"), USD), ProductStatus.ACTIVE);
        Product retiredKeyboard = product("Keyboard", null, new Money(new BigDecimal("30.00"), USD), ProductStatus.RETIRED);
        when(productRepository.findAll()).thenReturn(List.of(activeNotebook, activeMouse, retiredKeyboard));

        ProductPageResult result = service.search(new SearchProductsQuery(null, null, ProductStatus.ACTIVE, 0, 1));

        assertThat(result.items()).hasSize(1);
        assertThat(result.totalElements()).isEqualTo(2);
        assertThat(result.totalPages()).isEqualTo(2);
    }

    @Test
    void createCategoryRejectsDuplicateName() {
        when(categoryRepository.existsByName("Computers")).thenReturn(true);

        assertThatThrownBy(() -> service.createCategory(new CreateCategoryCommand("Computers", null)))
                .isInstanceOf(DuplicateCategoryException.class);
    }

    @Test
    void createCategoryHappyPath() {
        when(categoryRepository.existsByName("Computers")).thenReturn(false);

        CategoryId categoryId = service.createCategory(new CreateCategoryCommand("Computers", null));

        assertThat(categoryId).isNotNull();
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void getCategoriesReturnsQueryResults() {
        Category category = Category.create(CategoryId.newId(), "Computers", null);
        when(categoryRepository.findAll()).thenReturn(List.of(category));

        List<CategoryQueryResult> categories = service.getCategories();

        assertThat(categories).hasSize(1);
        assertThat(categories.get(0).name()).isEqualTo("Computers");
    }

    @Test
    void assignCategoryToUnknownCategoryThrows() {
        Product product = Product.create(ProductId.newId(), "Notebook", null, new Money(new BigDecimal("1500.00"), USD));
        CategoryId categoryId = CategoryId.newId();
        when(productRepository.findById(product.id())).thenReturn(Optional.of(product));
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.assignCategory(product.id(), categoryId))
                .isInstanceOf(CategoryNotFoundException.class);
    }

    @Test
    void assignCategoryAssignsAndPersists() {
        Product product = Product.create(ProductId.newId(), "Notebook", null, new Money(new BigDecimal("1500.00"), USD));
        Category category = Category.create(CategoryId.newId(), "Computers", null);
        when(productRepository.findById(product.id())).thenReturn(Optional.of(product));
        when(categoryRepository.findById(category.id())).thenReturn(Optional.of(category));

        service.assignCategory(product.id(), category.id());

        assertThat(product.categories()).containsExactly(category.id());
        verify(productRepository).save(product);
    }

    private Product product(String name, String description, Money price, ProductStatus status) {
        Product product = Product.create(ProductId.newId(), name, description, price);
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
